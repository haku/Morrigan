package net.sparktank.morrigan.model.tasks;

import java.io.File;
import java.util.List;

import net.sparktank.morrigan.model.IMorriganTask;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaItemList;
import net.sparktank.morrigan.model.TaskEventListener;
import net.sparktank.morrigan.model.TaskResult;
import net.sparktank.morrigan.model.TaskResult.TaskOutcome;


public class MediaFileCopyTask implements IMorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaItemList<? extends MediaItem> mediaItemList;
	private final List<? extends MediaItem> mediaSelection;
	private final File targetDirectory;
	
	public MediaFileCopyTask (MediaItemList<? extends MediaItem> mediaItemList, List<? extends MediaItem> mediaSelection, File targetDirectory) {
		this.mediaItemList = mediaItemList;
		this.mediaSelection = mediaSelection;
		this.targetDirectory = targetDirectory;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		return "Copying " + mediaSelection.size() + " files";
	}

	@Override
	public TaskResult run(TaskEventListener taskEventListener) {
		TaskResult ret;
		
		try {
			taskEventListener.beginTask("Copying", mediaSelection.size());
			
			for (MediaItem mi : mediaSelection) {
				taskEventListener.subTask(mi.getTitle());
				mediaItemList.copyMediaItemFile(mi, targetDirectory);
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
