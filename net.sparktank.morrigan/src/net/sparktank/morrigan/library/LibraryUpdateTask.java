package net.sparktank.morrigan.library;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.WeakHashMap;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.ChecksumHelper;
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
	
	/**
	 * TODO use monitor.worked(1);
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Updating library", IProgressMonitor.UNKNOWN);
		
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
				Stack<File> dirStack = new Stack<File>();
				dirStack.push(new File(source));
				
				while (!dirStack.isEmpty()) {
					File dirItem = dirStack.pop();
					File[] listFiles = dirItem.listFiles();
					
					monitor.subTask("Scanning "+dirItem.getAbsolutePath());
					
					for (File file : listFiles) {
						if (file.isDirectory()) {
							dirStack.push(file);
							
						} else if (file.isFile()) {
							String ext = file.getName();
							ext = ext.substring(ext.lastIndexOf(".")+1).toLowerCase();
							if (supportedFormats.contains(ext)) {
								try {
									if (library.addFile(file)) {
										filesAdded++;
									}
								} catch (MorriganException e) {
									// FIXME log this somewhere useful.
									e.printStackTrace();
								}
							}
						}
						
						if (monitor.isCanceled()) break;
					}
					
					if (monitor.isCanceled()) break;
				}
				
				if (monitor.isCanceled()) {
					System.out.println("Task was canceled desu~.");
					break;
				}
			}
		}
		
		IPlaybackEngine playbackEngine = null;
		
		monitor.subTask("Reading metadata");
		for (MediaItem mi : library.getMediaTracks()) {
			monitor.subTask("Reading metadata: " + mi.getTitle());
			
			// Existance test.
			File file = new File(mi.getFilepath());
			if (file.exists()) {
				// Hash code.
				if (mi.getHashcode() == 0) {
					try {
						long hash = ChecksumHelper.generateCrc32Checksum(mi.getFilepath());
						library.setTrackHashCode(mi, hash);
						System.out.println(Long.toHexString(hash) + " " + mi.getFilepath());
					} catch (Throwable t) {
						// FIXME log this somewhere useful.
						System.err.println("Throwable while marking track as missing '"+mi.getFilepath()+"': " + t.getMessage());
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
						System.err.println("Throwable while reading metadata for '"+mi.getFilepath()+"': " + t.getMessage());
					}
				}
				
			} else {
				try {
					library.setTrackMissing(mi, true);
				} catch (Throwable t) {
					// FIXME log this somewhere useful.
					System.err.println("Throwable while marking track as missing '"+mi.getFilepath()+"': " + t.getMessage());
				}
			}
			
			if (monitor.isCanceled()) {
				System.out.println("Task was canceled desu~.");
				break;
			}
		}
		
		// TODO : duplicate and missing files (and metadata merging).
		// TODO : anything special on re-finding of missing files?
		// TODO : vacuume DB?
		
		if (playbackEngine != null) {
			playbackEngine.finalise();
		}
		
		
		try {
			library.reRead();
		} catch (MorriganException e) {
			monitor.done();
			return new FailStatus("Failed to refresh the library.", e);
		}
		
		System.out.println("Added " + filesAdded + " files.");
		
		isFinished = true;
		monitor.done();
		return Status.OK_STATUS;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public class FailStatus implements IStatus {
		
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
