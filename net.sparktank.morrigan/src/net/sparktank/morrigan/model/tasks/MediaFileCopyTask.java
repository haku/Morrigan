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
		return "Copying files";
	}

	@Override
	public TaskResult run(TaskEventListener taskEventListener) {
		TaskResult ret;
		
		try {
			System.err.println("Copying " + mediaSelection.size() + " files...");
			
			for (MediaItem mi : mediaSelection) {
				mediaItemList.copyMediaItemFile(mi, targetDirectory);
			}
			
			System.err.println("Finished copying " + mediaSelection.size() + " files.");
			
			ret = new TaskResult(TaskOutcome.SUCCESS);
		}
		catch (Exception e) {
			ret = new TaskResult(TaskOutcome.FAILED, "Failed to copy all files.", e);
		}
		
		return ret;
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
