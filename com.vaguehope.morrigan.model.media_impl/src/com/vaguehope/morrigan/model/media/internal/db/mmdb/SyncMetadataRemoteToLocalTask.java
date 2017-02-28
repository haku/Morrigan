package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.tasks.TaskResult.TaskOutcome;
import com.vaguehope.morrigan.util.Objs;

public class SyncMetadataRemoteToLocalTask implements MorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final ILocalMixedMediaDb local;
	private final IRemoteMixedMediaDb remote;
	private final MediaFactory mediaFactory;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public SyncMetadataRemoteToLocalTask (final ILocalMixedMediaDb local, final IRemoteMixedMediaDb remote, final MediaFactory mediaFactory) {
		this.local = local;
		this.remote = remote;
		this.mediaFactory = mediaFactory;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getTitle () {
		// TODO make this more sensible.
		return "Sync metadata '" + this.local + "' x '" + this.remote + "' desu~";
	}

	@Override
	public TaskResult run (final TaskEventListener taskEventListener) {
		taskEventListener.onStart();
		TaskResult ret;
		try {
			final ILocalMixedMediaDb trans = this.mediaFactory.getLocalMixedMediaDbTransactional(this.local);
			try {
				trans.read();
				// FIXME add getByHashcode() to local DB.
				// Build list of all hashed local items.
				final Map<BigInteger, IMixedMediaItem> localItems = new HashMap<BigInteger, IMixedMediaItem>();
				for (final IMixedMediaItem localItem : trans.getAllDbEntries()) {
					final BigInteger hashcode = localItem.getHashcode();
					if (hashcode != null && !BigInteger.ZERO.equals(hashcode)) localItems.put(hashcode, localItem);
				}

				// All remote items.
				final List<IMixedMediaItem> remoteItems = this.remote.getAllDbEntries();

				// Describe what we are doing.
				final String taskTitle = "Synchronising metadata from " + this.remote.getListName() + " to " + this.local.getListName() + ".";
				taskEventListener.beginTask(taskTitle, remoteItems.size()); // Work total is number of remote items.

				// For each remote item, see if there is a local item to update.
				for (final IMixedMediaItem remoteItem : remoteItems) {
					final BigInteger hashcode = remoteItem.getHashcode();
					if (hashcode != null && !BigInteger.ZERO.equals(hashcode)) {
						final IMixedMediaItem localItem = localItems.get(hashcode);
						if (localItem != null) {
							taskEventListener.subTask(localItem.getTitle());
							syncMediaItems(trans, this.remote, remoteItem, localItem);
						}
					}
					taskEventListener.worked(1); // Increment one for each remote item.
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
		catch (final Exception e) {
			ret = new TaskResult(TaskOutcome.FAILED, "Throwable while sync metadata.", e);
		}

		return ret;
	}

	private static void syncMediaItems (final ILocalMixedMediaDb ldb, final IRemoteMixedMediaDb rdb, final IMixedMediaItem remoteItem, final IMixedMediaItem localItem) throws MorriganException {
		if (remoteItem.getStartCount() > localItem.getStartCount()) {
			ldb.setTrackStartCnt(localItem, remoteItem.getStartCount());
		}

		if (remoteItem.getEndCount() > localItem.getEndCount()) {
			ldb.setTrackEndCnt(localItem, remoteItem.getEndCount());
		}

		if (remoteItem.getDateAdded().getTime() > 0 && remoteItem.getDateAdded().getTime() < localItem.getDateAdded().getTime()) {
			ldb.setItemDateAdded(localItem, remoteItem.getDateAdded());
		}

		if (remoteItem.getDateLastPlayed() != null && remoteItem.getDateLastPlayed().getTime() > 0
				&& (localItem.getDateLastPlayed() == null || remoteItem.getDateLastPlayed().getTime() > localItem.getDateLastPlayed().getTime())) {
			ldb.setTrackDateLastPlayed(localItem, remoteItem.getDateLastPlayed());
		}

		final long rEnabledMod = remoteItem.enabledLastModified() != null && remoteItem.enabledLastModified().getTime() > 0L ? remoteItem.enabledLastModified().getTime() : 0L;
		final long lEnabledMod = localItem.enabledLastModified() != null && localItem.enabledLastModified().getTime() > 0L ? localItem.enabledLastModified().getTime() : 0L;
		if (remoteItem.isEnabled() != localItem.isEnabled() && rEnabledMod >= lEnabledMod) {
			ldb.setItemEnabled(localItem, remoteItem.isEnabled(), remoteItem.enabledLastModified());
		}

		if (localItem.getDuration() <= 0 && remoteItem.getDuration() > 0) {
			ldb.setTrackDuration(localItem, remoteItem.getDuration());
		}

		final List<MediaTag> rTags = rdb.getTagsIncludingDeleted(remoteItem);
		if (rTags != null && rTags.size() > 0) {
			// Index local tags.
			final List<MediaTag> lTagList = ldb.getTagsIncludingDeleted(localItem);
			final Map<TagKey, MediaTag> lTags = new HashMap<TagKey, MediaTag>(lTagList.size());
			for (final MediaTag lTag : lTagList) {
				lTags.put(new TagKey(lTag), lTag);
			}

			// Compare each remote tag to is local, if exists.
			for (final MediaTag rTag : rTags) {
				final MediaTag lTag = lTags.get(new TagKey(rTag));

				// If we have never seen this tag before, add if not deleted.
				if (lTag == null) {
					if (!rTag.isDeleted()) {
						addTag(ldb, localItem, rTag);
					}
					continue;
				}

				// If both deleted, then we are in agreement (don't care about matching modified dates).
				if (lTag.isDeleted() && rTag.isDeleted()) continue;

				// If remote tag is explicitly more recently modified than local tag, add it.
				final long lModified = lTag.getModified() != null && lTag.getModified().getTime() > 0L
						? lTag.getModified().getTime() : 0L;
				final long rModified = rTag.getModified() != null && rTag.getModified().getTime() > 0L
						? rTag.getModified().getTime() : 0L;
				if (rModified > lModified) {
					addTag(ldb, localItem, rTag);
				}
			}
		}
	}

	private static void addTag (final ILocalMixedMediaDb db, final IMixedMediaItem item, final MediaTag tag) throws MorriganException {
		final MediaTagClassification cls = tag.getClassification();
		final String clsString = cls == null ? null : cls.getClassification();
		db.addTag(item, tag.getTag(), tag.getType(), clsString, tag.getModified(), tag.isDeleted());
	}

	private static class TagKey {

		private final String tag;
		private final MediaTagType type;
		private final String cls;

		public TagKey (final MediaTag tag) {
			this.tag = tag.getTag();
			this.type = tag.getType();
			this.cls = tag.getClassification() != null ? tag.getClassification().getClassification() : null;
		}

		@Override
		public int hashCode () {
			return Objs.hash(this.tag, this.type, this.cls);
		}

		@Override
		public boolean equals (final Object obj) {
			if (obj == this) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final TagKey that = (TagKey) obj;
			return Objs.equals(this.tag, that.tag)
					&& Objs.equals(this.type, that.type)
					&& Objs.equals(this.cls, that.cls);
		}

	}

}
