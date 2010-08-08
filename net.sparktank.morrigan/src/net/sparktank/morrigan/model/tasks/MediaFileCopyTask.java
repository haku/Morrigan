package net.sparktank.morrigan.model.tasks;

import java.io.File;
import java.util.List;

import net.sparktank.morrigan.model.IMediaItemList;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.tasks.TaskResult.TaskOutcome;


public class MediaFileCopyTask<T extends MediaItem> implements IMorriganTask {
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
				this.mediaItemList.copyMediaItemFile(mi, this.targetDirectory);
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
