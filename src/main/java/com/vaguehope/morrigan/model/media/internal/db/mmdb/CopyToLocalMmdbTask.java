package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskOutcome;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.util.ChecksumHelper;

/**
 *
 * @param <T>
 *            the type of the source list.
 */
public class CopyToLocalMmdbTask implements MorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final MediaList fromList;
	private final Collection<MediaItem> itemsToCopy;
	private final IMediaItemDb toDb;
	private final Config config;

	public CopyToLocalMmdbTask (final MediaList fromList, final Collection<MediaItem> itemsToCopy, final IMediaItemDb toDb, final Config config) {
		this.fromList = fromList;
		this.itemsToCopy = itemsToCopy;
		this.toDb = toDb;
		this.config = config;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getTitle () {
		return "Copy " + this.itemsToCopy.size() + " items from " + this.fromList.getListName() + " to " + this.toDb.getListName();
	}

	@Override
	public TaskResult run (final TaskEventListener taskEventListener) {
		TaskResult ret = null;

		try {
			taskEventListener.onStart();
			taskEventListener.beginTask("Copying", this.itemsToCopy.size());

			final File coDir = getCheckoutDirectory(this.toDb);

			/*
			 * TODO rewrite this using a trans-clone?
			 */

			for (final MediaItem item : this.itemsToCopy) {
				taskEventListener.subTask(item.getTitle());

				final File coItemDir = getCheckoutItemDirectory(coDir, item);
				final File coFile = this.fromList.copyItemFile(item, coItemDir);
				if (!coFile.exists()) throw new FileNotFoundException("After fetching '" + item.getRemoteLocation() + "' can't find '" + coFile.getAbsolutePath() + "'.");

				// TODO FIXME re-write remote path with URL we fetched it from?  Perhaps this should be returned from copyItemFile()?

				// TODO these next few methods should really be combined into a single method in MediaItemDb.
				if (this.toDb.getDbLayer().hasFile(coFile).isKnown()) {
					this.toDb.getDbLayer().removeFile(coFile.getAbsolutePath());
				}
				final MediaItem addedItem = this.toDb.addFile(item.getMediaType(), coFile);
				addedItem.setFromMediaItem(item);
				this.toDb.persistTrackData(addedItem);

				taskEventListener.worked(1);
			}

			if (taskEventListener.isCanceled()) {
				taskEventListener.logMsg(this.toDb.getListName(), "Task was canceled desu~.");
				taskEventListener.done(TaskOutcome.CANCELLED);
				ret = new TaskResult(TaskOutcome.CANCELLED);
			}
			else {
				ret = new TaskResult(TaskOutcome.SUCCESS);
			}
		}
		catch (final Exception e) { // NOSONAR all task errors to be reported by UI.
			taskEventListener.done(TaskOutcome.FAILED);
			ret = new TaskResult(TaskOutcome.FAILED, "Throwable while fetching media.", e);
		}
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	// TODO extract this to Config class?
	private File getCheckoutDirectory (final IMediaItemDb db) {
		final File coDir = new File(this.config.getConfigDir(), "checkout");
		if (!coDir.exists()) {
			if (!coDir.mkdir()) {
				throw new RuntimeException("Failed to mkdir '" + coDir.getAbsolutePath() + "'.");
			}
		}

		final File dbCoDir = new File(coDir, db.getListName());
		if (!dbCoDir.exists()) {
			if (!dbCoDir.mkdir()) {
				throw new RuntimeException("Failed to mkdir '" + dbCoDir.getAbsolutePath() + "'.");
			}
		}

		return dbCoDir;
	}

	private static File getCheckoutItemDirectory (final File coDir, final MediaItem item) {
		final String srcPath = item.getRemoteLocation();

		final File dir = new File(coDir, ChecksumHelper.md5(srcPath).toString(16));
		if (!dir.exists()) {
			if (!dir.mkdir()) {
				throw new RuntimeException("Failed to mkdir '" + dir.getAbsolutePath() + "'.");
			}
		}

		return dir;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
