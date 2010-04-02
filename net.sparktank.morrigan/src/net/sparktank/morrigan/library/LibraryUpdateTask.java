package net.sparktank.morrigan.library;

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
import net.sparktank.morrigan.helpers.ConsoleHelper;
import net.sparktank.morrigan.model.media.MediaItem;
import net.sparktank.morrigan.model.media.MediaLibrary;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * FIXME prevent concurrent executions of this task.
 */
public class LibraryUpdateTask extends Job {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static WeakHashMap<LibraryUpdateTask, String> jobCache = new WeakHashMap<LibraryUpdateTask, String>();
	
	public static synchronized LibraryUpdateTask factory (MediaLibrary library) {
		LibraryUpdateTask ret = null;
		
		if (jobCache.containsValue(library.getListId())) {
			for (LibraryUpdateTask job : jobCache.keySet()) {
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
			ret = new LibraryUpdateTask(library);
			jobCache.put(ret, library.getListId());
			
		} else {
			ret = null;
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaLibrary library;
	private boolean isFinished = false;
	
	private LibraryUpdateTask (MediaLibrary library) {
		super("Update " + library.getListName());
		this.library = library;
		setUser(true);
	}
	
	public MediaLibrary getLibrary () {
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
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		ConsoleHelper.showConsole();
		ConsoleHelper.appendToConsole("Starting library scan...");
		
		int progress = 0;
		monitor.beginTask("Updating library", 100);
		
		monitor.subTask("Scanning sources");
		
		List<String> supportedFormats;
		try {
			supportedFormats = Arrays.asList(Config.getMediaFileTypes());
		} catch (MorriganException e) {
			monitor.done();
			return new FailStatus("Failed to retrieve list of supported formats.", e);
		}
		
		List<String> sources = null;
		try {
			sources = library.getSources();
		} catch (DbException e) {
			monitor.done();
			return new FailStatus("Failed to retrieve list of media sources.", e);
		}
		
		int filesAdded = 0;
		
		if (sources!=null) {
			for (String source : sources) {
				if (monitor.isCanceled()) break;
				
				Stack<File> dirStack = new Stack<File>();
				dirStack.push(new File(source));
				
				while (!dirStack.isEmpty()) {
					if (monitor.isCanceled()) break;
					
					File dirItem = dirStack.pop();
					File[] listFiles = dirItem.listFiles();
					if (listFiles != null) {
						monitor.subTask("Scanning " + dirItem.getAbsolutePath());
						
						for (File file : listFiles) {
							if (monitor.isCanceled()) break;
							
							if (file.isDirectory()) {
								dirStack.push(file);
								
							} else if (file.isFile()) {
								String ext = file.getName();
								ext = ext.substring(ext.lastIndexOf(".") + 1).toLowerCase();
								if (supportedFormats.contains(ext)) {
									try {
										if (library.addFile(file)) {
											ConsoleHelper.appendToConsole("[ADDED] " + file.getAbsolutePath());
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
						ConsoleHelper.appendToConsole("Failed to read directory: " + dirItem.getAbsolutePath());
					}
				}
			}
		} // End directory scanning.
		
		ConsoleHelper.appendToConsole("Added " + filesAdded + " files.");
		
		IPlaybackEngine playbackEngine = null;
		
		monitor.subTask("Reading metadata");
		int n = 0;
		int N = library.getCount();
		for (MediaItem mi : library.getMediaTracks()) {
			if (monitor.isCanceled()) break;
			monitor.subTask("Reading metadata: " + mi.getTitle());
			
			// Existance test.
			File file = new File(mi.getFilepath());
			if (file.exists()) {
				// If was missing, mark as found.
				if (mi.isMissing()) {
					try {
						ConsoleHelper.appendToConsole("[FOUND] " + mi.getFilepath());
						library.setTrackMissing(mi, false);
						// The file has gone and come back again.  We need to check the CRC32 is up to date.
						library.setTrackHashCode(mi, 0);
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
					ConsoleHelper.appendToConsole("[CHANGED] " + mi.getTitle());
					try {
						library.setTrackDateLastModified(mi, new Date(lastModified));
					} catch (Throwable t) {
						// FIXME log this somewhere useful.
						System.err.println("Throwable while writing track last modified date '"+mi.getFilepath()+"': " + t.getMessage());
					}
				}
				
				// Hash code.
				if (fileModified || mi.getHashcode() == 0) {
					try {
						long hash = ChecksumHelper.generateCrc32Checksum(mi.getFilepath());
						library.setTrackHashCode(mi, hash);
					} catch (Throwable t) {
						// FIXME log this somewhere useful.
						System.err.println("Throwable while setting track hash code '"+mi.getFilepath()+"': " + t.getMessage());
					}
				}
				
				// Duration.
				if (mi.getDuration()<=0) {
					if (playbackEngine == null) {
						try {
							playbackEngine = EngineFactory.makePlaybackEngine();
						} catch (Exception e) {
							monitor.done();
							return new FailStatus("Failed to create playback engine instance.", e);
						}
					}
					try {
						int d = playbackEngine.readFileDuration(mi.getFilepath());
						if (d>0) library.setTrackDuration(mi, d);
					} catch (Throwable t) {
						// FIXME log this somewhere useful.
						ConsoleHelper.appendToConsole("Throwable while reading metadata for '"+mi.getFilepath()+"': " + t.getMessage());
					}
				}
				
			} else { // The file is missing.
				if (!mi.isMissing()) {
					try {
						ConsoleHelper.appendToConsole("[MISSING] " + mi.getFilepath());
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
				monitor.worked(p - progress);
				progress = p;
			}
		} // End metadata scanning.
		
		if (playbackEngine != null) {
			playbackEngine.finalise();
		}
		
		/*
		 * Check for duplicates.
		 */
		monitor.subTask("Scanning for duplicates");
		
		Map<MediaItem, ScanOption> dupicateItems = new HashMap<MediaItem, ScanOption>();
		
		List<MediaItem> tracks = library.getMediaTracks();
		for (int i = 0; i < tracks.size(); i++) {
			if (monitor.isCanceled()) break;
			
			if (tracks.get(i).getHashcode() != 0) {
				boolean a = new File(tracks.get(i).getFilepath()).exists();
				for (int j = i + 1; j < tracks.size(); j++) {
					if (monitor.isCanceled()) break;
					
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
			monitor.subTask("Merging duplicate items");
			
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
				if (monitor.isCanceled()) break;
				
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
							ConsoleHelper.appendToConsole("[REMOVED] " + i.getFilepath());
							
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
			ConsoleHelper.appendToConsole("Found " + dupicateItems.size() + " duplicate items:");
			for (Entry<MediaItem, ScanOption> e : dupicateItems.entrySet()) {
				ConsoleHelper.appendToConsole(e.getValue() + " : " + e.getKey().getTitle());
			}
			
		} else {
			ConsoleHelper.appendToConsole("No duplicates found.");
		}
		
		// TODO : vacuum DB?
		
		if (monitor.isCanceled()) {
			ConsoleHelper.appendToConsole("Task was canceled desu~.");
		}
		
		isFinished = true;
		monitor.done();
		return Status.OK_STATUS;
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
	
	static public class FailStatus implements IStatus {
		
		private final String message;
		private final Exception e;

		public FailStatus (String message, Exception e) {
			this.message = message;
			this.e = e;
		}
		
		@Override
		public IStatus[] getChildren() {
			return null;
		}
		
		@Override
		public int getCode() {
			return 0;
		}
		
		@Override
		public Throwable getException() {
			return e;
		}
		
		@Override
		public String getMessage() {
			return message;
		}
		
		@Override
		public String getPlugin() {
			return null;
		}
		
		@Override
		public int getSeverity() {
			return 0;
		}
		
		@Override
		public boolean isMultiStatus() {
			return false;
		}
		
		@Override
		public boolean isOK() {
			return false;
		}
		
		@Override
		public boolean matches(int severityMask) {
			return false;
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
