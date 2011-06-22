package net.sparktank.morrigan.model.media.internal;

import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tasks.TaskResult;

public class SyncMetadataRemoteToLocalTask implements IMorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final ILocalMixedMediaDb local;
	private final IRemoteMixedMediaDb remote;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public SyncMetadataRemoteToLocalTask (ILocalMixedMediaDb local, IRemoteMixedMediaDb remote) {
		this.local = local;
		this.remote = remote;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		// TODO make this more sensible.
		return "Sync metadata '"+this.local+"' x '"+this.remote+"' desu~";
	}
	
	@Override
	public TaskResult run(TaskEventListener taskEventListener) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
