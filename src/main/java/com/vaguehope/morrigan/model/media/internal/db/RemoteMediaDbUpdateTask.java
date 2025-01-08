package com.vaguehope.morrigan.model.media.internal.db;

import com.vaguehope.morrigan.model.factory.RecyclingFactory;
import com.vaguehope.morrigan.model.media.RemoteMediaDb;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskOutcome;
import com.vaguehope.morrigan.tasks.TaskResult;

public class RemoteMediaDbUpdateTask implements MorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.

	public static class Factory extends RecyclingFactory<RemoteMediaDbUpdateTask, RemoteMediaDb, Void, RuntimeException> {

		protected Factory () {
			super(false);
		}

		@Override
		protected boolean isValidProduct (final RemoteMediaDbUpdateTask product) {
			return !product.isFinished();
		}

		@Override
		protected RemoteMediaDbUpdateTask makeNewProduct(final RemoteMediaDb material, final Void config) throws RuntimeException {
			return new RemoteMediaDbUpdateTask(material);
		}

	}

	public static final Factory FACTORY = new Factory();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final RemoteMediaDb rmmdb;

	private volatile boolean isFinished = false;

	RemoteMediaDbUpdateTask (final RemoteMediaDb rmmdb) {
		this.rmmdb = rmmdb;
	}

	@Override
	public String getTitle () {
		return "Update " + getRmmdb().getListName();
	}

	public RemoteMediaDb getRmmdb () {
		return this.rmmdb;
	}

	public boolean isFinished () {
		return this.isFinished;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public TaskResult run (final TaskEventListener taskEventListener) {
		TaskResult ret;

		try {
			this.rmmdb.setTaskEventListener(taskEventListener);
			this.rmmdb.forceDoRead();
			ret = new TaskResult(TaskOutcome.SUCCESS);

		}
		catch (Exception e) {
			ret = new TaskResult(TaskOutcome.FAILED, "Failed to refresh RMMDB.", e);

		}
		finally {
			this.isFinished = true;
			this.rmmdb.setTaskEventListener(null);
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
