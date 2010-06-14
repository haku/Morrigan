package net.sparktank.morrigan.model.library.remote;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.TaskEventListener;
import net.sparktank.morrigan.model.TaskResult;
import net.sparktank.morrigan.model.TaskResult.TaskOutcome;

public class RemoteLibraryUpdateTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.
	
	public static class Factory extends RecyclingFactory<RemoteLibraryUpdateTask, RemoteMediaLibrary, Void, RuntimeException> {
		
		protected Factory() {
			super(false);
		}
		
		@Override
		protected boolean isValidProduct(RemoteLibraryUpdateTask product) {
			return !product.isFinished();
		}
		
		@Override
		protected RemoteLibraryUpdateTask makeNewProduct(RemoteMediaLibrary material) {
			return new RemoteLibraryUpdateTask(material);
		}
		
	}
	
	public static final Factory FACTORY = new Factory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final RemoteMediaLibrary mediaLibrary;
	
	private volatile boolean isFinished = false;
	
	private RemoteLibraryUpdateTask(RemoteMediaLibrary mediaLibrary) {
		this.mediaLibrary = mediaLibrary;
	}
	
	public RemoteMediaLibrary getLibrary () {
		return mediaLibrary;
	}
	
	public boolean isFinished () {
		return isFinished;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public TaskResult run(TaskEventListener taskEventListener) {
		TaskResult ret;
		
		try {
			mediaLibrary.setTaskEventListener(taskEventListener);
			mediaLibrary.invalidateCache();
			mediaLibrary.reRead();
			ret = new TaskResult(TaskOutcome.SUCCESS);
			
		} catch (Exception e) {
			// TODO show error msg?
			e.printStackTrace();
			ret = new TaskResult(TaskOutcome.FAILED, "Failed to refresh library.", e);
			
		} finally {
			isFinished = true;
			mediaLibrary.setTaskEventListener(null);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
