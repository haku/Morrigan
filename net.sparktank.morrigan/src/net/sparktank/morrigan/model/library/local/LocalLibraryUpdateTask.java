package net.sparktank.morrigan.model.library.local;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.ChecksumHelper;
import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.library.DbException;
import net.sparktank.morrigan.model.library.MediaLibraryTrack;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tasks.TaskResult;
import net.sparktank.morrigan.model.tasks.TaskResult.TaskOutcome;

public class LocalLibraryUpdateTask implements IMorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.
	
	public static class Factory extends RecyclingFactory<LocalLibraryUpdateTask, LocalMediaLibrary, Void, RuntimeException> {
		
		protected Factory() {
			super(false);
		}
		
		@Override
		protected boolean isValidProduct(LocalLibraryUpdateTask product) {
			return !product.isFinished();
		}
		
		@Override
		protected LocalLibraryUpdateTask makeNewProduct(LocalMediaLibrary material) {
			return new LocalLibraryUpdateTask(material);
		}
		
	}
	
	public static final Factory FACTORY = new Factory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final LocalMediaLibrary library;
	
	private volatile boolean isFinished = false;
	
	private LocalLibraryUpdateTask (LocalMediaLibrary library) {
		this.library = library;
	}
	
	@Override
	public String getTitle() {
		return "Update " + getLibrary().getListName();
	}
	
	public LocalMediaLibrary getLibrary () {
		return library;
	}
	
	public boolean isFinished () {
		return isFinished;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private enum ScanOption {KEEP, DELREF, MOVEFILE};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO use monitor.worked(1);
	 */
	public TaskResult run(TaskEventListener taskEventListener) {
		TaskResult ret = null;
		try {
			taskEventListener.onStart();
			taskEventListener.logMsg(library.getListName(), "Starting scan...");
			taskEventListener.beginTask("Updating library", 100);
			
			// Scan directories for new files.
			ret = scanLibraryDirectories(taskEventListener);
			
			if (ret == null) {
				// Check known files exist and update metadata.
				ret = updateLibraryMetadata(taskEventListener, 25);
			}
			
			if (ret == null) {
				// Check for duplicate items and merge matching items.
				checkForDuplicates(taskEventListener, 25);
			}
			
			if (ret == null) {
				// Read track duration.
				ret = updateTrackMetadata(taskEventListener, 50);
			}
			
//			if (ret == null) {
//				 TODO : vacuum DB?
//			}
			
			if (ret == null) {
				if (taskEventListener.isCanceled()) {
					taskEventListener.logMsg(library.getListName(), "Task was canceled desu~.");
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
		
		isFinished = true;
		taskEventListener.done();
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Generic scanning.
	
	private TaskResult scanLibraryDirectories(TaskEventListener taskEventListener) {
		taskEventListener.subTask("Scanning sources");
		
		List<String> supportedFormats;
		try {
			supportedFormats = Arrays.asList(Config.getMediaFileTypes());
		} catch (MorriganException e) {
			taskEventListener.done();
			return new TaskResult(TaskOutcome.FAILED, "Failed to retrieve list of supported formats.", e);
		}
		
		List<String> sources = null;
		try {
			sources = library.getSources();
		} catch (DbException e) {
			taskEventListener.done();
			return new TaskResult(TaskOutcome.FAILED, "Failed to retrieve list of media sources.", e);
		}
		
		int filesAdded = 0;
		
		if (sources!=null) {
			for (String source : sources) {
				if (taskEventListener.isCanceled()) break;
				
				Stack<File> dirStack = new Stack<File>();
				dirStack.push(new File(source));
				
				while (!dirStack.isEmpty()) {
					if (taskEventListener.isCanceled()) break;
					
					File dirItem = dirStack.pop();
					File[] listFiles = dirItem.listFiles();
					if (listFiles != null) {
						taskEventListener.subTask("Scanning " + dirItem.getAbsolutePath());
						
						for (File file : listFiles) {
							if (taskEventListener.isCanceled()) break;
							
							if (file.isDirectory()) {
								dirStack.push(file);
							}
							else if (file.isFile()) {
								String ext = file.getName();
								ext = ext.substring(ext.lastIndexOf(".") + 1).toLowerCase();
								if (supportedFormats.contains(ext)) {
									try {
										if (library.addFile(file) != null) {
											taskEventListener.logMsg(library.getListName(), "[ADDED] " + file.getAbsolutePath());
											filesAdded++;
										}
									} catch (MorriganException e) {
										// FIXME log this somewhere useful.
										e.printStackTrace();
									}
								}
							}
						}
					}
					else {
						taskEventListener.logMsg(library.getListName(), "Failed to read directory: " + dirItem.getAbsolutePath());
					}
				}
			}
		} // End directory scanning.
		
		taskEventListener.logMsg(library.getListName(), "Added " + filesAdded + " files.");
		
		return null;
	}
	
	private TaskResult updateLibraryMetadata(TaskEventListener taskEventListener, int prgTotal) throws MorriganException {
		taskEventListener.subTask("Reading file metadata");
		
		int progress = 0;
		int n = 0;
		int N = library.getCount();
		
		List<MediaLibraryTrack> allLibraryEntries = library.getAllLibraryEntries();
		for (MediaLibraryTrack mi : allLibraryEntries) {
			if (taskEventListener.isCanceled()) break;
			taskEventListener.subTask("Reading file metadata: " + mi.getTitle());
			
			// Existence test.
			File file = new File(mi.getFilepath());
			if (file.exists()) {
				// If was missing, mark as found.
				if (mi.isMissing()) {
					try {
						taskEventListener.logMsg(library.getListName(), "[FOUND] " + mi.getFilepath());
						library.setTrackMissing(mi, false);
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
						taskEventListener.logMsg(library.getListName(), "[NEW] " + mi.getTitle());
					} else {
						taskEventListener.logMsg(library.getListName(), "[CHANGED] " + mi.getTitle());
					}
					
					try {
						library.setTrackDateLastModified(mi, new Date(lastModified));
					} catch (Throwable t) {
						// FIXME log this somewhere useful.
						System.err.println("Throwable while writing track last modified date '"+mi.getFilepath()+"': " + t.getMessage());
						t.printStackTrace();
					}
				}
				
				// Hash code.
				if (fileModified || mi.getHashcode() == 0) {
					long hash = 0;
					
					try {
						hash = ChecksumHelper.generateCrc32Checksum(mi.getFilepath());
						
					} catch (Throwable t) {
						// FIXME log this somewhere useful.
						System.err.println("Throwable while generating checksum for '"+mi.getFilepath()+": " + t.getMessage());
						t.printStackTrace();
					}
					
					if (hash != 0) {
						try {
							library.setTrackHashCode(mi, hash);
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
						taskEventListener.logMsg(library.getListName(), "[MISSING] " + mi.getFilepath());
						library.setTrackMissing(mi, true);
					} catch (Throwable t) {
						// FIXME log this somewhere useful.
						System.err.println("Throwable while marking track as missing '"+mi.getFilepath()+"': " + t.getMessage());
					}
				}
			}
			
			n++;
			int p = (n * prgTotal) / N;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		} // End metadata scanning.
		
		return null;
	}
	
	private void checkForDuplicates(TaskEventListener taskEventListener, int prgTotal) throws MorriganException {
		taskEventListener.subTask("Scanning for duplicates");
		Map<MediaLibraryTrack, ScanOption> dupicateItems = new HashMap<MediaLibraryTrack, ScanOption>();
		
		List<MediaLibraryTrack> tracks = library.getAllLibraryEntries();
		
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
			int p = (n * prgTotal) / N;
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
			for (MediaItem mi : dupicateItems.keySet()) {
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
				Map<MediaLibraryTrack, ScanOption> items = new HashMap<MediaLibraryTrack, ScanOption>();
				for (MediaLibraryTrack mi : dupicateItems.keySet()) {
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
					
					MediaLibraryTrack keep = null;
					for (MediaLibraryTrack i : items.keySet()) {
						if (items.get(i) == ScanOption.KEEP) keep = i;
					}
					items.remove(keep);
					
					if (keep == null) throw new NullPointerException("Something very bad happened.");
					
					/* Now merge:
					 * start count, end count,
					 * added data, last played data.
					 * Then remove missing tracks from library.
					 */
					for (MediaLibraryTrack i : items.keySet()) {
//						boolean success = false;
//						try {
						// FIXME fix this transaction stuff.
							/*
							 * FIXME TODO get some form of lock for this transaction?
							 * What if the user changes something while we do this?
							 */
//							library.setAutoCommit(false);
							
							library.incTrackStartCnt(keep, i.getStartCount());
							library.incTrackEndCnt(keep, i.getEndCount());
							
							if (i.getDateAdded() != null) {
								if (keep.getDateAdded() == null
										|| keep.getDateAdded().getTime() > i.getDateAdded().getTime()) {
									library.setDateAdded(keep, i.getDateAdded());
								}
							}
							
							if (i.getDateLastPlayed() != null) {
								if (keep.getDateLastPlayed() == null
										|| keep.getDateLastPlayed().getTime() < i.getDateLastPlayed().getTime()) {
									library.setDateLastPlayed(keep, i.getDateLastPlayed());
								}
							}
							
							if (library.hasTags(i)) {
								library.moveTags(i, keep);
							}
							
							if (keep.getDuration() <= 0 && i.getDuration() > 0) {
								library.setTrackDuration(keep, i.getDuration());
							}
							
							if (i.isMissing() && keep.isEnabled() && !i.isEnabled()) {
								library.setTrackEnabled(keep, i.isEnabled());
							}
							
							library.removeMediaTrack(i);
							taskEventListener.logMsg(library.getListName(), "[REMOVED] " + i.getFilepath());
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
					for (MediaItem i : items.keySet()) {
						dupicateItems.remove(i);
					}
				}
				
			} // End metadata merging.
			
			/*
			 * Print out what we are left with.
			 */
			taskEventListener.logMsg(library.getListName(), "Performed " + countMerges + " mergers.");
			taskEventListener.logMsg(library.getListName(), "Found " + dupicateItems.size() + " duplicate items:");
			for (Entry<MediaLibraryTrack, ScanOption> e : dupicateItems.entrySet()) {
				taskEventListener.logMsg(library.getListName(), e.getValue() + " : " + e.getKey().getTitle());
			}
		}
		else {
			taskEventListener.logMsg(library.getListName(), "No duplicates found.");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Track specific scanning.
	
	private TaskResult updateTrackMetadata (TaskEventListener taskEventListener, int prgTotal) throws MorriganException {
		taskEventListener.subTask("Reading track metadata");
		
		IPlaybackEngine playbackEngine = null;
		
		int progress = 0;
		int n = 0;
		int N = library.getCount();
		
		List<MediaLibraryTrack> allLibraryEntries = library.getAllLibraryEntries();
		for (MediaLibraryTrack mi : allLibraryEntries) {
			if (taskEventListener.isCanceled()) break;
			taskEventListener.subTask("Reading track metadata: " + mi.getTitle());
			
			if (mi.getDuration()<=0) {
				if (!library.isMarkedAsUnreadable(mi)) {
					if (mi.isEnabled()) {
						File file = new File(mi.getFilepath());
						if (file.exists()) {
							if (playbackEngine == null) {
								try {
									playbackEngine = EngineFactory.makePlaybackEngine();
								} catch (Exception e) {
									taskEventListener.done();
									return new TaskResult(TaskOutcome.FAILED, "Failed to create playback engine instance.", e);
								}
							}
							
							try {
								int d = playbackEngine.readFileDuration(mi.getFilepath());
								if (d>0) library.setTrackDuration(mi, d);
							}
							catch (Throwable t) {
								// FIXME log this somewhere useful.
								taskEventListener.logMsg(library.getListName(), "Error while reading metadata for '"+mi.getFilepath()+"': " + t.getMessage());

								// Tag track as unreadable.
//								library.markAsUnreadabled(mi); // FIXME what if the user wants to try again?
							}
						} // End exists test.
					} else { // If marked as disabled.
						taskEventListener.logMsg(library.getListName(), "Ignoring disabled file '"+mi.getFilepath()+"'.");
					}
				} else { // If tagged as unreadable.
					taskEventListener.logMsg(library.getListName(), "Ignoring unreadable file '"+mi.getFilepath()+"'.");
				}
			}// End duration > 0 test.
			
			n++;
			int p = (n * prgTotal) / N;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		} // End metadata scanning.
		
		if (playbackEngine != null) {
			playbackEngine.finalise();
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Helper methods.
	
	static private int countEntriesInMap (Map<?, ?> map, Object value) {
		int n = 0;
		for ( Entry<?, ?> e : map.entrySet()) {
			if (e.getValue().equals(value)) n++;
		}
		return n;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
