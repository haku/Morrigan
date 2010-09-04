package net.sparktank.morrigan.model.media.impl;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tasks.TaskResult;
import net.sparktank.morrigan.model.tasks.TaskResult.TaskOutcome;

public class RemoteMixedMediaDbUpdateTask implements IMorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.
	
	public static class Factory extends RecyclingFactory<RemoteMixedMediaDbUpdateTask, RemoteMixedMediaDb, Void, RuntimeException> {
		
		protected Factory() {
			super(false);
		}
		
		@Override
		protected boolean isValidProduct(RemoteMixedMediaDbUpdateTask product) {
			return !product.isFinished();
		}
		
		@Override
		protected RemoteMixedMediaDbUpdateTask makeNewProduct(RemoteMixedMediaDb material) {
			return new RemoteMixedMediaDbUpdateTask(material);
		}
		
	}
	
	public static final Factory FACTORY = new Factory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final RemoteMixedMediaDb rmmdb;
	
	private volatile boolean isFinished = false;
	
	RemoteMixedMediaDbUpdateTask(RemoteMixedMediaDb rmmdb) {
		this.rmmdb = rmmdb;
	}
	
	@Override
	public String getTitle() {
		return "Update " + getRmmdb().getListName();
	}
	
	public RemoteMixedMediaDb getRmmdb () {
		return this.rmmdb;
	}
	
	public boolean isFinished () {
		return this.isFinished;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public TaskResult run(TaskEventListener taskEventListener) {
		TaskResult ret;
		
		try {
			this.rmmdb.setTaskEventListener(taskEventListener);
			this.rmmdb.forceDoRead();
			ret = new TaskResult(TaskOutcome.SUCCESS);
			
		} catch (Exception e) {
			ret = new TaskResult(TaskOutcome.FAILED, "Failed to refresh RMMDB.", e);
			
		} finally {
			this.isFinished = true;
			this.rmmdb.setTaskEventListener(null);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
