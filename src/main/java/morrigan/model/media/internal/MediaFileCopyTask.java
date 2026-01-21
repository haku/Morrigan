package morrigan.model.media.internal;

import java.io.File;
import java.util.List;

import morrigan.model.media.MediaItem;
import morrigan.model.media.MediaList;
import morrigan.tasks.MorriganTask;
import morrigan.tasks.TaskEventListener;
import morrigan.tasks.TaskOutcome;
import morrigan.tasks.TaskResult;

public class MediaFileCopyTask implements MorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final MediaList mediaItemList;
	private final List<MediaItem> mediaSelection;
	private final File targetDirectory;

	public MediaFileCopyTask (final MediaList mediaItemList, final List<MediaItem> mediaSelection, final File targetDirectory) {
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

			for (final MediaItem mi : this.mediaSelection) {
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
