package net.sparktank.morrigan.model.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.ChecksumHelper;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.TaskEventListener;
import net.sparktank.morrigan.model.library.LocalLibraryUpdateTask.TaskResult.TaskOutcome;

public class LocalLibraryUpdateTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static WeakHashMap<LocalLibraryUpdateTask, String> jobCache = new WeakHashMap<LocalLibraryUpdateTask, String>();
	
	public static synchronized LocalLibraryUpdateTask factory (LocalMediaLibrary library) {
		LocalLibraryUpdateTask ret = null;
		
		if (jobCache.containsValue(library.getListId())) {
			for (LocalLibraryUpdateTask job : jobCache.keySet()) {
				if (job.getLibrary().getListId().equals(library.getListId())) {
					ret = job;
				}
			}
		}
		
		// if its finished it does not count.
		if (ret != null && ret.isFinished()) {
			jobCache.remove(ret);
			ret = null;
		}
		
		if (ret == null) {
			ret = new LocalLibraryUpdateTask(library);
			jobCache.put(ret, library.getListId());
			
		} else {
			ret = null;
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public class TaskResult {
		
		static public enum TaskOutcome {SUCCESS, FAILED, CANCELED};
		
		private TaskOutcome outcome;
		private String errMsg;
		private Throwable errThr;
		
		public TaskResult (TaskOutcome outcome) {
			this.outcome = outcome;
		}
		
		public TaskResult (TaskOutcome outcome, String errMsg, Throwable errThr) {
			this.outcome = outcome;
			this.errMsg = errMsg;
			this.errThr = errThr;
		}
		
		public void setOutcome(TaskOutcome outcome) {
			this.outcome = outcome;
		}
		public TaskOutcome getOutcome() {
			return outcome;
		}
		
		public void setErrMsg(String errMsg) {
			this.errMsg = errMsg;
		}
		public String getErrMsg() {
			return errMsg;
		}
		
		public void setErrThr(Throwable errThr) {
			this.errThr = errThr;
		}
		public Throwable getErrThr() {
			return errThr;
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final LocalMediaLibrary library;
	
	private boolean isFinished = false;
	
	private LocalLibraryUpdateTask (LocalMediaLibrary library) {
		this.library = library;
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
		taskEventListener.onStart();
		taskEventListener.logMsg(library.getListName(), "Starting scan...");
		
		int progress = 0;
		taskEventListener.beginTask("Updating library", 100);
		
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
								
							} else if (file.isFile()) {
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
					} else {
						taskEventListener.logMsg(library.getListName(), "Failed to read directory: " + dirItem.getAbsolutePath());
					}
				}
			}
		} // End directory scanning.
		
		taskEventListener.logMsg(library.getListName(), "Added " + filesAdded + " files.");
		
		IPlaybackEngine playbackEngine = null;
		
		taskEventListener.subTask("Reading metadata");
		int n = 0;
		int N = library.getCount();
		for (MediaLibraryItem mi : library.getMediaTracks()) {
			if (taskEventListener.isCanceled()) break;
			taskEventListener.subTask("Reading metadata: " + mi.getTitle());
			
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
				
				// Duration.
				if (mi.getDuration()<=0) {
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
					} catch (Throwable t) {
						// FIXME log this somewhere useful.
						taskEventListener.logMsg(library.getListName(), "Throwable while reading metadata for '"+mi.getFilepath()+"': " + t.getMessage());
					}
				}
				
			} else { // The file is missing.
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
			int p = (n * 100) / N;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		} // End metadata scanning.
		
		if (playbackEngine != null) {
			playbackEngine.finalise();
		}
		
		/*
		 * Check for duplicates.
		 */
		taskEventListener.subTask("Scanning for duplicates");
		
		Map<MediaItem, ScanOption> dupicateItems = new HashMap<MediaItem, ScanOption>();
		
		List<MediaLibraryItem> tracks = library.getMediaTracks();
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
								
							} else if (a != b) { // Only one exists.
								if (!dupicateItems.containsKey(tracks.get(i))) {
									dupicateItems.put(tracks.get(i),
											a ? ScanOption.KEEP : ScanOption.DELREF);
								}
								if (!dupicateItems.containsKey(tracks.get(j))) {
									dupicateItems.put(tracks.get(j),
											b ? ScanOption.KEEP : ScanOption.DELREF);
								}
								
							} else { // Neither exist.
								// They are both missing.  Don't worry about it.
							}
							
						}
					}
				}
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
			
			/*
			 * Resolve each unique hashcode.
			 */
			for (Long l : hashcodes) {
				if (taskEventListener.isCanceled()) break;
				
				/*
				 * Find all the entries for this hashcode.
				 */
				Map<MediaItem, ScanOption> items = new HashMap<MediaItem, ScanOption>();
				for (MediaItem mi : dupicateItems.keySet()) {
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
					
					MediaItem keep = null;
					for (MediaItem i : items.keySet()) {
						if (items.get(i) == ScanOption.KEEP) keep = i;
					}
					items.remove(keep);
					
					if (keep == null) throw new NullPointerException("Something very bad happened.");
					
					/* Now merge:
					 * start count, end count,
					 * added data, last played data.
					 * Then remove missing tracks from library.
					 */
					for (MediaItem i : items.keySet()) {
						try {
							// TODO this should be done in a tranasaction!
							
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
							
							library.removeMediaTrack(i);
							taskEventListener.logMsg(library.getListName(), "[REMOVED] " + i.getFilepath());
							
						} catch (Throwable t) {
							// FIXME log this somewhere useful.
							System.err.println("Throwable while setting merged metadate for '"+i.getFilepath()+"': " + t.getMessage());
							t.printStackTrace();
						}
						
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
			 * Print out what are left with.
			 */
			taskEventListener.logMsg(library.getListName(), "Found " + dupicateItems.size() + " duplicate items:");
			for (Entry<MediaItem, ScanOption> e : dupicateItems.entrySet()) {
				taskEventListener.logMsg(library.getListName(), e.getValue() + " : " + e.getKey().getTitle());
			}
			
		} else {
			taskEventListener.logMsg(library.getListName(), "No duplicates found.");
		}
		
		// TODO : vacuum DB?
		
		if (taskEventListener.isCanceled()) {
			taskEventListener.logMsg(library.getListName(), "Task was canceled desu~.");
		}
		
		isFinished = true;
		taskEventListener.done();
		return new TaskResult(TaskOutcome.SUCCESS);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private int countEntriesInMap (Map<?, ?> map, Object value) {
		int n = 0;
		for ( Entry<?, ?> e : map.entrySet()) {
			if (e.getValue().equals(value)) n++;
		}
		return n;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
