package com.vaguehope.morrigan.server;

import java.net.URI;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.server.model.PullRemoteToLocal;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
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
	private final Config config;

	public AsyncActions (final AsyncTasksRegister asyncTasksRegister, final MediaFactory mediaFactory, final Config config) {
		this.asyncTasksRegister = asyncTasksRegister;
		this.mediaFactory = mediaFactory;
		this.config = config;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public AsyncTask scheduleMmdbScan (final IMediaItemDb mmdb) {
		if (mmdb.getType() == MediaListType.LOCALMMDB) {
			return scheduleLocalMmdbScan(mmdb);
		}
		else if (mmdb instanceof IRemoteMixedMediaDb) {
			IRemoteMixedMediaDb rmmdb = (IRemoteMixedMediaDb) mmdb;
			return scheduleRemoteMmdbScan(rmmdb);
		}
		else {
			throw new IllegalArgumentException("Unknown type: '"+mmdb.getClass().getName()+"'.");
		}
	}

	public AsyncTask scheduleLocalMmdbScan (final IMediaItemDb mmdb) {
		final MorriganTask task = this.mediaFactory.getLocalMixedMediaDbUpdateTask(mmdb);
		if (task != null) {
			return this.asyncTasksRegister.scheduleTask(task);
		}
		else {
			throw new IllegalArgumentException("Failed to get task object from factory method.");
		}
	}

	public AsyncTask scheduleRemoteMmdbScan (final IRemoteMixedMediaDb mmdb) {
		final MorriganTask task = this.mediaFactory.getRemoteMixedMediaDbUpdateTask(mmdb);
		if (task != null) {
			return this.asyncTasksRegister.scheduleTask(task);
		}
		else {
			throw new IllegalArgumentException("Failed to get task object from factory method.");
		}
	}

	public void syncMetaData (final IMediaItemDb ldb, final IRemoteMixedMediaDb rdb) {
		MorriganTask task = this.mediaFactory.getSyncMetadataRemoteToLocalTask(ldb, rdb);
		if (task != null) {
			this.asyncTasksRegister.scheduleTask(task);
		}
		else {
			throw new IllegalArgumentException("Failed to get task object from factory method.");
		}
	}

	public AsyncTask scheduleMmdbPull (final IMediaItemDb db, final String remote) throws DbException {
		final URI remoteUri = db.getRemote(remote);
		if (remoteUri == null) throw new IllegalArgumentException("Invalid remote name: " + remote);
		return this.asyncTasksRegister.scheduleTask(new PullRemoteToLocal(db, remoteUri, this.mediaFactory, this.config));
	}

}
