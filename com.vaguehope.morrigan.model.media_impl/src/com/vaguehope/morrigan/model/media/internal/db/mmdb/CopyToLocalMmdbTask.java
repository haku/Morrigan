package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.tasks.TaskResult.TaskOutcome;
import com.vaguehope.morrigan.util.ChecksumHelper;

/**
 *
 * @param <T>
 *            the type of the source list.
 */
public class CopyToLocalMmdbTask<T extends IMediaItem> implements MorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final IMediaItemList<T> fromList;
	private final Collection<T> itemsToCopy;
	private final ILocalMixedMediaDb toDb;

	public CopyToLocalMmdbTask (final IMediaItemList<T> fromList, final Collection<T> itemsToCopy, final ILocalMixedMediaDb toDb) {
		this.fromList = fromList;
		this.itemsToCopy = itemsToCopy;
		this.toDb = toDb;
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

			File coDir = getCheckoutDirectory(this.toDb);

			/*
			 * TODO rewrite this using a trans-clone?
			 */

			for (T item : this.itemsToCopy) {
				taskEventListener.subTask(item.getTitle());

				File coItemDir = getCheckoutItemDirectory(coDir, item);
				File coFile = this.fromList.copyItemFile(item, coItemDir);
				if (!coFile.exists()) throw new FileNotFoundException("After fetching '" + item.getRemoteLocation() + "' can't find '" + coFile.getAbsolutePath() + "'.");

				// TODO FIXME re-write remote path with URL we fetched it from?  Perhaps this should be returned from copyItemFile()?

				// TODO these next few methods should really be combined into a single method in MediaItemDb.
				if (this.toDb.getDbLayer().hasFile(coFile).isKnown()) {
					this.toDb.getDbLayer().removeFile(coFile.getAbsolutePath());
				}
				IMixedMediaItem addedItem = this.toDb.addFile(coFile);
				addedItem.setFromMediaItem(item);
				this.toDb.persistTrackData(addedItem);

				taskEventListener.worked(1);
			}

			if (taskEventListener.isCanceled()) {
				taskEventListener.logMsg(this.toDb.getListName(), "Task was canceled desu~.");
				ret = new TaskResult(TaskOutcome.CANCELED);
			}
			else {
				ret = new TaskResult(TaskOutcome.SUCCESS);
			}

		}
		catch (Exception e) { // NOSONAR all task errors to be reported by UI.
			ret = new TaskResult(TaskOutcome.FAILED, "Throwable while fetching media.", e);
		}

		taskEventListener.done();
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	// TODO extract this to config?
	private static File getCheckoutDirectory (final ILocalMixedMediaDb db) {
		String configDir = Config.getConfigDir();

		File coDir = new File(configDir, "checkout");
		if (!coDir.exists()) {
			if (!coDir.mkdir()) {
				throw new RuntimeException("Failed to mkdir '" + coDir.getAbsolutePath() + "'.");
			}
		}

		File dbCoDir = new File(coDir, db.getListName());
		if (!dbCoDir.exists()) {
			if (!dbCoDir.mkdir()) {
				throw new RuntimeException("Failed to mkdir '" + dbCoDir.getAbsolutePath() + "'.");
			}
		}

		return dbCoDir;
	}

	private static File getCheckoutItemDirectory (final File coDir, final IMediaItem item) {
		String srcPath = item.getRemoteLocation();

		File dir = new File(coDir, ChecksumHelper.md5String(srcPath));
		if (!dir.exists()) {
			if (!dir.mkdir()) {
				throw new RuntimeException("Failed to mkdir '" + dir.getAbsolutePath() + "'.");
			}
		}

		return dir;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
