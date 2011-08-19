package net.sparktank.morrigan.model.media.internal;

import java.io.File;
import java.util.List;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.tasks.IMorriganTask;
import com.vaguehope.morrigan.model.tasks.TaskEventListener;
import com.vaguehope.morrigan.model.tasks.TaskResult;
import com.vaguehope.morrigan.model.tasks.TaskResult.TaskOutcome;



public class MediaFileCopyTask<T extends IMediaItem> implements IMorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final IMediaItemList<T> mediaItemList;
	private final List<T> mediaSelection;
	private final File targetDirectory;
	
	public MediaFileCopyTask (IMediaItemList<T> mediaItemList, List<T> mediaSelection, File targetDirectory) {
		this.mediaItemList = mediaItemList;
		this.mediaSelection = mediaSelection;
		this.targetDirectory = targetDirectory;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		return "Copying " + this.mediaSelection.size() + " files";
	}

	@Override
	public TaskResult run(TaskEventListener taskEventListener) {
		TaskResult ret;
		
		try {
			taskEventListener.beginTask("Copying", this.mediaSelection.size());
			
			for (T mi : this.mediaSelection) {
				taskEventListener.subTask(mi.getTitle());
				this.mediaItemList.copyItemFile(mi, this.targetDirectory);
				taskEventListener.worked(1);
				
				if (taskEventListener.isCanceled()) {
					break;
				}
			}
			
			if (taskEventListener.isCanceled()) {
				ret = new TaskResult(TaskOutcome.CANCELED);
			} else {
				ret = new TaskResult(TaskOutcome.SUCCESS);
			}
			
			taskEventListener.done();
		}
		catch (Exception e) {
			ret = new TaskResult(TaskOutcome.FAILED, "Failed to copy all files.", e);
		}
		
		return ret;
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
