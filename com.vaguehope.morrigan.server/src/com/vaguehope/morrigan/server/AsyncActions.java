package com.vaguehope.morrigan.server;

import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.tasks.IMorriganTask;

/**
 * Set of helper functions for interacting
 * when there is no GUI.
 *
 */
public class AsyncActions {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final MediaFactory mediaFactory;
	private final AsyncTasksRegister asyncTasksRegister;

	public AsyncActions (AsyncTasksRegister asyncTasksRegister, MediaFactory mediaFactory) {
		this.asyncTasksRegister = asyncTasksRegister;
		this.mediaFactory = mediaFactory;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void scheduleMmdbScan (final IMixedMediaDb mmdb) {
		if (mmdb instanceof ILocalMixedMediaDb) {
			ILocalMixedMediaDb lmmdb = (ILocalMixedMediaDb) mmdb;
			scheduleMmdbScan(lmmdb);
		}
		else if (mmdb instanceof IRemoteMixedMediaDb) {
			IRemoteMixedMediaDb rmmdb = (IRemoteMixedMediaDb) mmdb;
			scheduleRemoteMmdbScan(rmmdb);
		}
		else {
			throw new IllegalArgumentException("Unknown type: '"+mmdb.getClass().getName()+"'.");
		}
	}

	public void scheduleMmdbScan (final ILocalMixedMediaDb mmdb) {
		final IMorriganTask task = this.mediaFactory.getLocalMixedMediaDbUpdateTask(mmdb);
		if (task != null) {
			this.asyncTasksRegister.scheduleTask(task);
		}
		else {
			throw new IllegalArgumentException("Failed to get task object from factory method.");
		}
	}

	public void scheduleRemoteMmdbScan (final IRemoteMixedMediaDb mmdb) {
		final IMorriganTask task = this.mediaFactory.getRemoteMixedMediaDbUpdateTask(mmdb);
		if (task != null) {
			this.asyncTasksRegister.scheduleTask(task);
		}
		else {
			throw new IllegalArgumentException("Failed to get task object from factory method.");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void syncMetaData (ILocalMixedMediaDb ldb, IRemoteMixedMediaDb rdb) {
		IMorriganTask task = this.mediaFactory.getSyncMetadataRemoteToLocalTask(ldb, rdb);
		if (task != null) {
			this.asyncTasksRegister.scheduleTask(task);
		}
		else {
			throw new IllegalArgumentException("Failed to get task object from factory method.");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
