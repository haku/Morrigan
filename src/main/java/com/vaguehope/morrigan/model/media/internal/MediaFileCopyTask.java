package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.util.List;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.tasks.TaskResult.TaskOutcome;

public class MediaFileCopyTask<T extends IMediaItem> implements MorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final IMediaItemList<T> mediaItemList;
	private final List<T> mediaSelection;
	private final File targetDirectory;

	public MediaFileCopyTask (final IMediaItemList<T> mediaItemList, final List<T> mediaSelection, final File targetDirectory) {
		this.mediaItemList = mediaItemList;
		this.mediaSelection = mediaSelection;
		this.targetDirectory = targetDirectory;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getTitle () {
		return "Copying " + this.mediaSelection.size() + " files";
	}

	@Override
	public TaskResult run (final TaskEventListener taskEventListener) {
		TaskResult ret;

		taskEventListener.onStart();
		try {
			taskEventListener.beginTask("Copying", this.mediaSelection.size());

			for (final T mi : this.mediaSelection) {
				taskEventListener.subTask(mi.getTitle());
				this.mediaItemList.copyItemFile(mi, this.targetDirectory);
				taskEventListener.worked(1);

				if (taskEventListener.isCanceled()) break;
			}

			if (taskEventListener.isCanceled()) {
				ret = new TaskResult(TaskOutcome.CANCELED);
			}
			else {
				ret = new TaskResult(TaskOutcome.SUCCESS);
			}
		}
		catch (final Exception e) {
			ret = new TaskResult(TaskOutcome.FAILED, "Failed to copy all files.", e);
		}

		taskEventListener.done();
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
