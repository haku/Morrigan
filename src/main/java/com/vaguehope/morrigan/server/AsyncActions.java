package com.vaguehope.morrigan.server;

import com.vaguehope.morrigan.model.media.ListRef.ListType;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.tasks.AsyncTask;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.tasks.MorriganTask;

/**
 * Set of helper functions for interacting
 * when there is no GUI.
 *
 */
public class AsyncActions {

	private final MediaFactory mediaFactory;
	private final AsyncTasksRegister asyncTasksRegister;

	public AsyncActions (final AsyncTasksRegister asyncTasksRegister, final MediaFactory mediaFactory) {
		this.asyncTasksRegister = asyncTasksRegister;
		this.mediaFactory = mediaFactory;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public AsyncTask scheduleMmdbScan (final MediaDb mmdb) {
		if (mmdb.getListRef().getType() == ListType.LOCAL) {
			return scheduleLocalMmdbScan(mmdb);
		}
		else {
			throw new IllegalArgumentException("Unknown type: '"+mmdb.getClass().getName()+"'.");
		}
	}

	public AsyncTask scheduleLocalMmdbScan (final MediaDb mmdb) {
		final MorriganTask task = this.mediaFactory.getLocalMixedMediaDbUpdateTask(mmdb);
		if (task != null) {
			return this.asyncTasksRegister.scheduleTask(task);
		}
		else {
			throw new IllegalArgumentException("Failed to get task object from factory method.");
		}
	}

}
