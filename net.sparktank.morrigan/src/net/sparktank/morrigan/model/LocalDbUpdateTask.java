package net.sparktank.morrigan.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.ChecksumHelper;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemStorageLayer;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tasks.TaskResult;
import net.sparktank.morrigan.model.tasks.TaskResult.TaskOutcome;
import net.sparktank.sqlitewrapper.DbException;

public abstract class LocalDbUpdateTask<Q extends IMediaItemDb<Q, ? extends IMediaItemStorageLayer<T>,T>, T extends IMediaItem> implements IMorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected enum ScanOption {KEEP, DELREF, MOVEFILE};
	
	protected class OpResult {
		
		private final Throwable t;
		private final String msg;
		private final boolean faital;
		
		public OpResult (String msg, Throwable t) {
			this.msg = msg;
			this.t = t;
			this.faital = false;
		}
		
		public OpResult (String msg, Throwable t, boolean faital) {
			this.msg = msg;
			this.t = t;
			this.faital = faital;
		}
		
		public String getMsg() {
			return this.msg;
		}
		
		public Throwable getThrowable () {
			return this.t;
		}
		
		public boolean isFaital() {
			return this.faital;
		}
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Q itemList;
	
	private volatile boolean isFinished = false;
	
	public LocalDbUpdateTask (Q itemList) {
		this.itemList = itemList;
	}
	
	@Override
	public String getTitle() {
		return "Update " + getItemList().getListName();
	}
	
	public Q getItemList () {
		return this.itemList;
	}
	
	public boolean isFinished () {
		return this.isFinished;
	}
	public void setFinished () {
		this.isFinished = true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Main run method.
	
	/**
	 * TODO use monitor.worked(1);
	 */
	@Override
	public TaskResult run(TaskEventListener taskEventListener) {
		TaskResult ret = null;
		List<T> changedItems = new LinkedList<T>();
		
		try {
			taskEventListener.onStart();
			taskEventListener.logMsg(this.getItemList().getListName(), "Starting scan...");
			taskEventListener.beginTask("Updating library", 100);
			
			// Scan directories for new files.
			ret = scanLibraryDirectories(taskEventListener);
			
			if (ret == null) {
				// Check known files exist and update metadata.
				ret = updateLibraryMetadata(taskEventListener, 25, changedItems);
			}
			
			if (ret == null) {
				// Check for duplicate items and merge matching items.
				checkForDuplicates(taskEventListener, 25);
			}
			
			if (ret == null) {
				// Read track duration.
				ret = updateTrackMetadata1(taskEventListener, 25);
			}
			
			if (ret == null) {
				// Read track tags duration.
				ret = updateTrackMetadata2(taskEventListener, 25, changedItems);
			}
			
//			if (ret == null) {
//				 TODO : vacuum DB?
//			}
			
			if (ret == null) {
				if (taskEventListener.isCanceled()) {
					taskEventListener.logMsg(this.getItemList().getListName(), "Task was canceled desu~.");
					ret = new TaskResult(TaskOutcome.CANCELED);
				}
				else {
					ret = new TaskResult(TaskOutcome.SUCCESS);
				}
			}
		}
		catch (Throwable t) {
			ret = new TaskResult(TaskOutcome.FAILED, "Throwable while updating library.", t);
		}
		
		this.setFinished();
		taskEventListener.done();
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Generic scanning.
	
	private TaskResult scanLibraryDirectories(TaskEventListener taskEventListener) throws DbException, MorriganException {
		taskEventListener.subTask("Scanning sources");
		
		List<String> supportedFormats;
		try {
			supportedFormats = Arrays.asList(getItemFileExtensions());
		} catch (MorriganException e) {
			taskEventListener.done();
			return new TaskResult(TaskOutcome.FAILED, "Failed to retrieve list of supported formats.", e);
		}
		
		List<String> sources = null;
		try {
			sources = this.getItemList().getSources();
		} catch (DbException e) {
			taskEventListener.done();
			return new TaskResult(TaskOutcome.FAILED, "Failed to retrieve list of media sources.", e);
		}
		
		List<File> filesToAdd = new LinkedList<File>();
		int filesAdded = 0;
		
		if (sources!=null) {
			for (String source : sources) {
				if (taskEventListener.isCanceled()) break;
				
				Stack<File> dirStack = new Stack<File>();
				dirStack.push(new File(source));
				
				while (!dirStack.isEmpty()) {
					if (taskEventListener.isCanceled()) break;
					
					File dirItem = dirStack.pop();
					taskEventListener.subTask("(" + filesToAdd.size() + ") Scanning " + dirItem.getAbsolutePath());
					
					File[] arrFiles = dirItem.listFiles();
					if (arrFiles != null) {
						
						List<File> listFiles = new ArrayList<File>(arrFiles.length);
						for (File file : arrFiles) {
							listFiles.add(file);
						}
						Collections.sort(listFiles);
						
						for (File file : listFiles) {
							if (taskEventListener.isCanceled()) break;
							
							if (file.isDirectory()) {
								dirStack.push(file);
							}
							else if (file.isFile()) {
								String ext = file.getName();
								ext = ext.substring(ext.lastIndexOf(".") + 1).toLowerCase();
								if (supportedFormats.contains(ext)) {
									if (!this.getItemList().hasFile(file)) {
										filesToAdd.add(file);
									}
								}
							}
							
//							if (filesToAdd.size() >= 100) {
//								this.getItemList().addFiles(filesToAdd);
//								filesAdded = filesAdded + filesToAdd.size();
//								filesToAdd.clear();
//							}
						}
					}
					else {
						taskEventListener.logMsg(this.getItemList().getListName(), "Failed to read directory: " + dirItem.getAbsolutePath());
					}
				}
			}
		} // End directory scanning.
		
		if (filesToAdd.size() > 0) {
			taskEventListener.logMsg(this.getItemList().getListName(), "Addeding " + filesToAdd.size() + " files to DB...");
			
			Q transClone = this.getItemList().getTransactionalClone();
			try {
				transClone.addFiles(filesToAdd);
				filesAdded = filesAdded + filesToAdd.size();
			}
			finally {
				taskEventListener.logMsg(this.getItemList().getListName(), "Committing " + filesAdded + " inserts to DB...");
				try {
					transClone.commitOrRollback();
				} finally {
					transClone.dispose();
				}
			}
		}
		
		// Make main connection pick up changes to DB.
		this.getItemList().forceRead();
		
		taskEventListener.logMsg(this.getItemList().getListName(), "Added " + filesAdded + " files.");
		
		return null;
	}
	
	abstract protected String[] getItemFileExtensions () throws MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private TaskResult updateLibraryMetadata(TaskEventListener taskEventListener, int prgTotal, List<T> changedItems) throws DbException {
		if (changedItems.size() > 0) throw new IllegalArgumentException("changedItems list must be empty.");
		
		taskEventListener.subTask("Reading file metadata");
		
		int progress = 0;
		int n = 0;
		int N = this.getItemList().getCount();
		
		List<T> allLibraryEntries = this.getItemList().getAllDbEntries();
		for (T mi : allLibraryEntries) {
			if (taskEventListener.isCanceled()) break;
			taskEventListener.subTask("Reading file metadata: " + mi.getTitle());
			
			// Existence test.
			File file = new File(mi.getFilepath());
			if (file.exists()) {
				// If was missing, mark as found.
				if (mi.isMissing()) {
					try {
						taskEventListener.logMsg(this.getItemList().getListName(), "[FOUND] " + mi.getFilepath());
						this.getItemList().setItemMissing(mi, false);
					} catch (Throwable t) {
						// FIXME log this somewhere useful.
						System.err.println("Throwable while marking track as found '"+mi.getFilepath()+"': " + t.getMessage());
					}
				}
				
				// Last modified date and hash code.
				long lastModified = file.lastModified();
				boolean fileModified = false;
				if (mi.getDateLastModified() == null || mi.getDateLastModified().getTime() != lastModified ) {
					fileModified = true;
					
					if (mi.getDateLastModified() == null) {
						taskEventListener.logMsg(this.getItemList().getListName(), "[NEW] " + mi.getTitle());
					} else {
						taskEventListener.logMsg(this.getItemList().getListName(), "[CHANGED] " + mi.getTitle());
					}
					
					try {
						this.getItemList().setItemDateLastModified(mi, new Date(lastModified));
					} catch (Throwable t) {
						// FIXME log this somewhere useful.
						System.err.println("Throwable while writing track last modified date '"+mi.getFilepath()+"': " + t.getMessage());
						t.printStackTrace();
					}
					
					changedItems.add(mi);
				}
				
				// Hash code.
				if (fileModified || mi.getHashcode() == 0) {
					long hash = 0;
					
					try {
						hash = ChecksumHelper.generateCrc32Checksum(mi.getFilepath());
					}
					catch (Throwable t) {
						// FIXME log this somewhere useful.
						System.err.println("Throwable while generating checksum for '"+mi.getFilepath()+": " + t.getMessage());
						t.printStackTrace();
					}
					
					if (hash != 0) {
						try {
							this.getItemList().setItemHashCode(mi, hash);
						} catch (Throwable t) {
							// FIXME log this somewhere useful.
							System.err.println("Throwable while setting hash code for '"+mi.getFilepath()+"' to '"+hash+"': " + t.getMessage());
							t.printStackTrace();
						}
					}
					
				}
			}
			else { // The file is missing.
				if (!mi.isMissing()) {
					try {
						taskEventListener.logMsg(this.getItemList().getListName(), "[MISSING] " + mi.getFilepath());
						this.getItemList().setItemMissing(mi, true);
					}
					catch (Throwable t) {
						// FIXME log this somewhere useful.
						System.err.println("Throwable while marking track as missing '"+mi.getFilepath()+"': " + t.getMessage());
					}
				}
			}
			
			n++;
			int p = N > 0 ? (n * prgTotal) / N : 0;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		} // End metadata scanning.
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void checkForDuplicates(TaskEventListener taskEventListener, int prgTotal) throws MorriganException, DbException {
		taskEventListener.subTask("Scanning for duplicates");
		Map<T, ScanOption> dupicateItems = new HashMap<T, ScanOption>();
		
		List<T> tracks = this.getItemList().getAllDbEntries();
		
		int progress = 0;
		int n = 0;
		int N = tracks.size();
		
		for (int i = 0; i < tracks.size(); i++) {
			if (taskEventListener.isCanceled()) break;
			
			if (tracks.get(i).getHashcode() != 0) {
				boolean a = new File(tracks.get(i).getFilepath()).exists();
				for (int j = i + 1; j < tracks.size(); j++) {
					if (taskEventListener.isCanceled()) break;
					
					if (tracks.get(j).getHashcode() != 0) {
						if (tracks.get(i).getHashcode() == tracks.get(j).getHashcode()) {
							boolean b = new File(tracks.get(j).getFilepath()).exists();
							
							if (a && b) { // Both exist.
								// TODO prompt to move the newer one?
								if (!dupicateItems.containsKey(tracks.get(i))) {
									dupicateItems.put(tracks.get(i), ScanOption.KEEP);
								}
								if (!dupicateItems.containsKey(tracks.get(j))) {
									dupicateItems.put(tracks.get(j), ScanOption.MOVEFILE);
								}
							}
							else if (a != b) { // Only one exists.
								if (!dupicateItems.containsKey(tracks.get(i))) {
									dupicateItems.put(tracks.get(i),
											a ? ScanOption.KEEP : ScanOption.DELREF);
								}
								if (!dupicateItems.containsKey(tracks.get(j))) {
									dupicateItems.put(tracks.get(j),
											b ? ScanOption.KEEP : ScanOption.DELREF);
								}
							}
							else { // Neither exist.
								// They are both missing.  Don't worry about it.
							}
							
						}
					}
				}
			}
			
			n++;
			int p = N > 0 ? (n * prgTotal) / N : 0;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		} // End duplicate item scanning.
		
		if (dupicateItems.size() > 0) {
			taskEventListener.subTask("Merging duplicate items");
			
			/*
			 * Make a list of all the unique hashcodes we know.
			 */
			List<Long> hashcodes = new ArrayList<Long>();
			for (IMediaItem mi : dupicateItems.keySet()) {
				Long l = Long.valueOf(mi.getHashcode());
				if (!hashcodes.contains(l)) {
					hashcodes.add(l);
				}
			}
			
			int countMerges = 0;
			
			/*
			 * Resolve each unique hashcode.
			 */
			for (Long l : hashcodes) {
				if (taskEventListener.isCanceled()) break;
				
				/*
				 * Find all the entries for this hashcode.
				 */
				Map<T, ScanOption> items = new HashMap<T, ScanOption>();
				for (T mi : dupicateItems.keySet()) {
					if (mi.getHashcode() == l.longValue()) {
						items.put(mi, dupicateItems.get(mi));
					}
				}
				
				/*
				 * If there is only one entry that still exists,
				 * merge metadata and remove bad references.
				 * This is the only supported merge case at the
				 * moment.
				 */
				if (countEntriesInMap(items, ScanOption.KEEP) == 1
						&& countEntriesInMap(items, ScanOption.DELREF) == items.size()-1) {
					
					T keep = null;
					for (T i : items.keySet()) {
						if (items.get(i) == ScanOption.KEEP) keep = i;
					}
					items.remove(keep);
					
					if (keep == null) throw new NullPointerException("Something very bad happened.");
					
					/* Now merge:
					 * start count, end count,
					 * added data, last played data.
					 * Then remove missing tracks from library.
					 */
					for (T i : items.keySet()) {
//						boolean success = false;
//						try {
						// FIXME fix this transaction stuff.
							/*
							 * FIXME TODO get some form of lock for this transaction?
							 * What if the user changes something while we do this?
							 */
//							library.setAutoCommit(false);
							
							mergeItems(keep, i);
							
							this.getItemList().removeItem(i);
							taskEventListener.logMsg(this.getItemList().getListName(), "[REMOVED] " + i.getFilepath());
							countMerges++;
							
//							library.commit();
//							success = true;
//						}
//						finally {
//							if (!success) {
//								library.rollback();
//							}
//							library.setAutoCommit(true);
//						}
					}
					
					/*
					 * Removed processed entries from duplicate
					 * items list.
					 */
					dupicateItems.remove(keep);
					for (IMediaItem i : items.keySet()) {
						dupicateItems.remove(i);
					}
				}
				
			} // End metadata merging.
			
			/*
			 * Print out what we are left with.
			 */
			taskEventListener.logMsg(this.getItemList().getListName(), "Performed " + countMerges + " mergers.");
			taskEventListener.logMsg(this.getItemList().getListName(), "Found " + dupicateItems.size() + " duplicate items:");
			for (Entry<T, ScanOption> e : dupicateItems.entrySet()) {
				taskEventListener.logMsg(this.getItemList().getListName(), e.getValue() + " : " + e.getKey().getTitle());
			}
		}
		else {
			taskEventListener.logMsg(this.getItemList().getListName(), "No duplicates found.");
		}
	}
	
	abstract protected void mergeItems (T itemToKeep, T itemToBeRemove) throws MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private TaskResult updateTrackMetadata1 (TaskEventListener taskEventListener, int prgTotal) throws MorriganException, DbException {
		taskEventListener.subTask("Reading track metadata");
		
		int progress = 0;
		int n = 0;
		int N = this.getItemList().getCount();
		
		List<T> allLibraryEntries = this.getItemList().getAllDbEntries();
		
		try {
    		for (T mi : allLibraryEntries) {
    			if (taskEventListener.isCanceled()) break;
    			taskEventListener.subTask("Reading track metadata: " + mi.getTitle());
    			
    			if (shouldTrackMetaData1(taskEventListener, this.getItemList(), mi)) {
    				if (mi.isEnabled()) {
    					File file = new File(mi.getFilepath());
    					if (file.exists()) {
    						OpResult ret = readTrackMetaData1(this.getItemList(), mi, file);
    						if (ret != null) {
    							if (ret.isFaital()) {
    								throw ret.getThrowable();
    							}
    							
    							// FIXME log this somewhere useful.
    							taskEventListener.logError(this.getItemList().getListName(), "Error while reading metadata for '"+mi.getFilepath()+"'.", ret.getThrowable());
    							
    							// Tag track as unreadable.
    							//library.markAsUnreadabled(mi); // FIXME what if the user wants to try again?
    						}
    					} // End exists test.
    				} else { // If marked as disabled.
    					taskEventListener.logMsg(this.getItemList().getListName(), "Ignoring disabled file '"+mi.getFilepath()+"'.");
    				}
    			}// End duration > 0 test.
    			
    			n++;
    			int p = N > 0 ? (n * prgTotal) / N : 0;
    			if (p > progress) {
    				taskEventListener.worked(p - progress);
    				progress = p;
    			}
    		} // End file metadata scanning.
		}
		catch (Throwable t) {
			throw new MorriganException("Faital error while reading metadata from files.", t);
		}
		finally {
			try {
				cleanUpAfterTrackMetaData1();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		
		return null;
	}
	
	abstract protected boolean shouldTrackMetaData1 (TaskEventListener taskEventListener, Q library, T item) throws MorriganException;
	abstract protected OpResult readTrackMetaData1 (Q library, T item, File file);
	abstract protected void cleanUpAfterTrackMetaData1 ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private TaskResult updateTrackMetadata2 (TaskEventListener taskEventListener, int prgTotal, List<T> changedItems) {
		taskEventListener.subTask("Reading more track metadata");
		
		int progress = 0;
		int n = 0;
		int N = this.getItemList().getCount();
		
		for (T mlt : changedItems) {
			if (taskEventListener.isCanceled()) break;
			taskEventListener.subTask("Reading more track metadata: " + mlt.getTitle());
			
			try {
				File file = new File(mlt.getFilepath());
				if (file.exists()) {
					readTrackMetaData2(this.getItemList(), mlt, file);
				}
			}
			catch (Throwable t) {
				taskEventListener.logError(this.getItemList().getListName(), "Error while reading more metadata for '"+mlt.getFilepath()+"'.", t);
				t.printStackTrace();
			}
			
			n++;
			int p = N > 0 ? (n * prgTotal) / N : 0;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		} // End track metadata scanning.
		
		return null;
	}
	
	abstract protected void readTrackMetaData2 (Q library, T item, File file) throws Throwable;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Helper methods.
	
	static protected int countEntriesInMap (Map<?, ?> map, Object value) {
		int n = 0;
		for ( Entry<?, ?> e : map.entrySet()) {
			if (e.getValue().equals(value)) n++;
		}
		return n;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
