package com.vaguehope.morrigan.server.model;

import java.net.URI;
import java.util.UUID;

import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.tasks.TaskResult.TaskOutcome;

public class PullRemoteToLocal implements MorriganTask {

	private final ILocalMixedMediaDb ldb;
	private final URI remoteUri;
	private final MediaFactory mediaFactory;
	private final AsyncTasksRegister asyncTasksRegister;

	public PullRemoteToLocal (final ILocalMixedMediaDb localDb, final URI remoteUri,
			final MediaFactory mediaFactory, final AsyncTasksRegister asyncTasksRegister) {
		this.ldb = localDb;
		this.remoteUri = remoteUri;
		this.mediaFactory = mediaFactory;
		this.asyncTasksRegister = asyncTasksRegister;
	}

	@Override
	public String getTitle () {
		return String.format("Pull %s <-- %s", this.ldb.getListName(), this.remoteUri);
	}

	@Override
	public TaskResult run (final TaskEventListener taskEventListener) {
		taskEventListener.onStart();
		taskEventListener.beginTask(getTitle(), 1);
		try {
			taskEventListener.subTask("Fetching UUID");
			final RemoteDbMetadataFetcher metadata = new RemoteDbMetadataFetcher(this.remoteUri);
			metadata.fetch();
			final UUID uuid = metadata.getUuid();

			taskEventListener.subTask("Fetching metadata");
			final String file = RemoteMixedMediaDbHelper.getFullPathToMmdb(uuid.toString());
			final RemoteHostDetails details = new RemoteHostDetails(this.remoteUri.toURL());
			final IRemoteMixedMediaDb rdb = RemoteMixedMediaDbFactory.getNew(file, details);

			taskEventListener.subTask("Fetching DB");
			rdb.forceDoRead();

			taskEventListener.subTask("Syncing metadata");
			final MorriganTask syncTask = this.mediaFactory.getSyncMetadataRemoteToLocalTask(this.ldb, rdb);
			if (syncTask != null) {
				this.asyncTasksRegister.scheduleTask(syncTask);
			}
			else {
				throw new IllegalArgumentException("Failed to get task object from factory method.");
			}

			taskEventListener.done();
			return new TaskResult(TaskOutcome.SUCCESS);
		}
		catch (final Exception e) {
			return new TaskResult(TaskOutcome.FAILED, "Throwable while pulling metadata.", e);
		}
	}

}
