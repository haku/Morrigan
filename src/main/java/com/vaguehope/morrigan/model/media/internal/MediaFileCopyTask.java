package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.util.List;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskOutcome;
import com.vaguehope.morrigan.tasks.TaskResult;

public class MediaFileCopyTask implements MorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final IMediaItemList mediaItemList;
	private final List<IMediaItem> mediaSelection;
	private final File targetDirectory;

	public MediaFileCopyTask (final IMediaItemList mediaItemList, final List<IMediaItem> mediaSelection, final File targetDirectory) {
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

			for (final IMediaItem mi : this.mediaSelection) {
				taskEventListener.subTask(mi.getTitle());
				this.mediaItemList.copyItemFile(mi, this.targetDirectory);
				taskEventListener.worked(1);

				if (taskEventListener.isCanceled()) break;
			}

			if (taskEventListener.isCanceled()) {
				taskEventListener.done(TaskOutcome.CANCELLED);
				ret = new TaskResult(TaskOutcome.CANCELLED);
			}
			else {
				ret = new TaskResult(TaskOutcome.SUCCESS);
			}
		}
		catch (final Exception e) {
			taskEventListener.done(TaskOutcome.FAILED);
			ret = new TaskResult(TaskOutcome.FAILED, "Failed to copy all files.", e);
		}
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
