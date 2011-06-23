package net.sparktank.morrigan.model.media.internal;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tasks.TaskResult;
import net.sparktank.morrigan.model.tasks.TaskResult.TaskOutcome;
import net.sparktank.sqlitewrapper.DbException;

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
	public String getTitle () {
		// TODO make this more sensible.
		return "Sync metadata '"+this.local+"' x '"+this.remote+"' desu~";
	}
	
	@Override
	public TaskResult run (TaskEventListener taskEventListener) {
		TaskResult ret;
		try {
			final ILocalMixedMediaDb trans = this.local.getTransactionalClone();
			try {
				// FIXME add getByHashcode() to local DB.
				final Map<BigInteger, IMixedMediaItem> localItems = new HashMap<BigInteger, IMixedMediaItem>();
				for (IMixedMediaItem localItem : trans.getAllDbEntries()) {
					localItems.put(localItem.getHashcode(), localItem);
				}
				
				final List<IMixedMediaItem> remoteItems = this.remote.getAllDbEntries();
				taskEventListener.beginTask("Sync'ing", remoteItems.size());
				for (IMixedMediaItem remoteItem : remoteItems) {
					final IMixedMediaItem localItem = localItems.get(remoteItem.getHashcode());
					if (localItem != null) {
						taskEventListener.subTask(localItem.getTitle());
						syncMediaItems(trans, remoteItem, localItem);
					}
					taskEventListener.worked(1);
					if (taskEventListener.isCanceled()) break;
				}
				
				trans.commitOrRollback();
				this.local.forceRead(); // TODO replace by using bulk-update methods?  e.g. in MixedMediaDbFeedParser.
				
				if (taskEventListener.isCanceled()) {
					taskEventListener.logMsg(this.getTitle(), "Sync task was canceled desu~."); // TODO is this quite right?
					ret = new TaskResult(TaskOutcome.CANCELED);
				}
				else {
					ret = new TaskResult(TaskOutcome.SUCCESS);
				}
				taskEventListener.done();
			}
			finally {
				trans.dispose();
			}
		}
		catch (DbException e) {
			ret = new TaskResult(TaskOutcome.FAILED, "Throwable while sync metadata.", e);
		}
		catch (MorriganException e) {
			ret = new TaskResult(TaskOutcome.FAILED, "Throwable while sync metadata.", e);
		}
		
		return ret;
	}
	
	/**
	 * TODO also sync tags.
	 */
	private void syncMediaItems (final ILocalMixedMediaDb db, IMixedMediaItem remoteItem, IMixedMediaItem localItem) throws MorriganException {
		if (remoteItem.getStartCount() > localItem.getStartCount()) {
			db.setTrackStartCnt(localItem, remoteItem.getStartCount());
		}
		
		if (remoteItem.getEndCount() > localItem.getEndCount()) {
			db.setTrackEndCnt(localItem, remoteItem.getEndCount());
		}
		
		if (remoteItem.getDateAdded().getTime() > 0 && remoteItem.getDateAdded().getTime() < localItem.getDateAdded().getTime()) {
			db.setItemDateAdded(localItem, remoteItem.getDateAdded());
		}
		
		if (
				remoteItem.getDateLastPlayed() != null && remoteItem.getDateLastPlayed().getTime() > 0
				&& (localItem.getDateLastPlayed() == null || remoteItem.getDateLastPlayed().getTime() > localItem.getDateLastPlayed().getTime())
				) {
			db.setTrackDateLastPlayed(localItem, remoteItem.getDateLastPlayed());
		}
		
		// TODO FIXME does remote DB in fact sync enabled state??
		if (remoteItem.isEnabled() != localItem.isEnabled()) {
			db.setItemEnabled(localItem, remoteItem.isEnabled());
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
