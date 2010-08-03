package net.sparktank.morrigan.model.tracks.library.remote;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tasks.TaskResult;
import net.sparktank.morrigan.model.tasks.TaskResult.TaskOutcome;

public class RemoteLibraryUpdateTask implements IMorriganTask {
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
	
	RemoteLibraryUpdateTask(RemoteMediaLibrary mediaLibrary) {
		this.mediaLibrary = mediaLibrary;
	}
	
	@Override
	public String getTitle() {
		return "Update " + getLibrary().getListName();
	}
	
	public RemoteMediaLibrary getLibrary () {
		return this.mediaLibrary;
	}
	
	public boolean isFinished () {
		return this.isFinished;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public TaskResult run(TaskEventListener taskEventListener) {
		TaskResult ret;
		
		try {
			this.mediaLibrary.setTaskEventListener(taskEventListener);
			this.mediaLibrary.forceDoRead();
			ret = new TaskResult(TaskOutcome.SUCCESS);
			
		} catch (Exception e) {
			ret = new TaskResult(TaskOutcome.FAILED, "Failed to refresh library.", e);
			
		} finally {
			this.isFinished = true;
			this.mediaLibrary.setTaskEventListener(null);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
