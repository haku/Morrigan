package net.sparktank.morrigan.model.media.internal.db.mmdb;

import net.sparktank.morrigan.model.factory.RecyclingFactory;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tasks.TaskResult;
import net.sparktank.morrigan.model.tasks.TaskResult.TaskOutcome;

public class RemoteMixedMediaDbUpdateTask implements IMorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.
	
	public static class Factory extends RecyclingFactory<RemoteMixedMediaDbUpdateTask, IRemoteMixedMediaDb, Void, RuntimeException> {
		
		protected Factory() {
			super(false);
		}
		
		@Override
		protected boolean isValidProduct(RemoteMixedMediaDbUpdateTask product) {
			return !product.isFinished();
		}
		
		@Override
		protected RemoteMixedMediaDbUpdateTask makeNewProduct(IRemoteMixedMediaDb material) {
			return new RemoteMixedMediaDbUpdateTask(material);
		}
		
	}
	
	public static final Factory FACTORY = new Factory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final IRemoteMixedMediaDb rmmdb;
	
	private volatile boolean isFinished = false;
	
	RemoteMixedMediaDbUpdateTask(IRemoteMixedMediaDb rmmdb) {
		this.rmmdb = rmmdb;
	}
	
	@Override
	public String getTitle() {
		return "Update " + getRmmdb().getListName();
	}
	
	public IRemoteMixedMediaDb getRmmdb () {
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
