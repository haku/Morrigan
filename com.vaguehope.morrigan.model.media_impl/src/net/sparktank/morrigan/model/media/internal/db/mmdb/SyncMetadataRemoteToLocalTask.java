package net.sparktank.morrigan.model.media.internal.db.mmdb;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.tasks.IMorriganTask;
import com.vaguehope.morrigan.model.tasks.TaskEventListener;
import com.vaguehope.morrigan.model.tasks.TaskResult;
import com.vaguehope.morrigan.model.tasks.TaskResult.TaskOutcome;
import com.vaguehope.sqlitewrapper.DbException;

import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.MediaTag;

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
					BigInteger hashcode = localItem.getHashcode();
					if (hashcode != null && !BigInteger.ZERO.equals(hashcode)) localItems.put(hashcode, localItem);
				}
				
				final List<IMixedMediaItem> remoteItems = this.remote.getAllDbEntries();
				taskEventListener.beginTask("Sync'ing", remoteItems.size());
				for (IMixedMediaItem remoteItem : remoteItems) {
					BigInteger hashcode = remoteItem.getHashcode();
					if (hashcode != null && !BigInteger.ZERO.equals(hashcode)) {
						final IMixedMediaItem localItem = localItems.get(hashcode);
						if (localItem != null) {
							taskEventListener.subTask(localItem.getTitle());
							syncMediaItems(trans, this.remote, remoteItem, localItem);
						}
						taskEventListener.worked(1);
						if (taskEventListener.isCanceled()) break;
					}
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
	
	private static void syncMediaItems (final ILocalMixedMediaDb ldb, IRemoteMixedMediaDb rdb, IMixedMediaItem remoteItem, IMixedMediaItem localItem) throws MorriganException {
		if (remoteItem.getStartCount() > localItem.getStartCount()) {
			ldb.setTrackStartCnt(localItem, remoteItem.getStartCount());
		}
		
		if (remoteItem.getEndCount() > localItem.getEndCount()) {
			ldb.setTrackEndCnt(localItem, remoteItem.getEndCount());
		}
		
		if (remoteItem.getDateAdded().getTime() > 0 && remoteItem.getDateAdded().getTime() < localItem.getDateAdded().getTime()) {
			ldb.setItemDateAdded(localItem, remoteItem.getDateAdded());
		}
		
		if (
				remoteItem.getDateLastPlayed() != null && remoteItem.getDateLastPlayed().getTime() > 0
				&& (localItem.getDateLastPlayed() == null || remoteItem.getDateLastPlayed().getTime() > localItem.getDateLastPlayed().getTime())
				) {
			ldb.setTrackDateLastPlayed(localItem, remoteItem.getDateLastPlayed());
		}
		
		if (remoteItem.isEnabled() != localItem.isEnabled()) {
			ldb.setItemEnabled(localItem, remoteItem.isEnabled());
		}
		
		List<MediaTag> rTags = rdb.getTags(remoteItem);
		if (rTags != null && rTags.size() > 0) {
			for (MediaTag rTag : rTags) {
				ldb.addTag(localItem, rTag.getTag(), rTag.getType(), rTag.getClassification().getClassification());
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
