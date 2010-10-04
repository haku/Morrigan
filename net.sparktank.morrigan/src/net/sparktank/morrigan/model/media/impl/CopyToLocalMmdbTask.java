package net.sparktank.morrigan.model.media.impl;

import java.io.File;
import java.util.Collection;

import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemList;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tasks.TaskResult;
import net.sparktank.morrigan.model.tasks.TaskResult.TaskOutcome;

/**
 * 
 * @param <T> the type of the source list.
 */
public class CopyToLocalMmdbTask<T extends IMediaItem> implements IMorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final IMediaItemList<T> fromList;
	private final Collection<T> itemsToCopy;
	private final LocalMixedMediaDb toDb;
	
	public CopyToLocalMmdbTask (IMediaItemList<T> fromList, Collection<T> itemsToCopy, LocalMixedMediaDb toDb) {
		this.fromList = fromList;
		this.itemsToCopy = itemsToCopy;
		this.toDb = toDb;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		return "Copying media from " + this.fromList.getListName();
	}
	
	@Override
	public TaskResult run(TaskEventListener taskEventListener) {
		TaskResult ret = null;
		
		try {
			taskEventListener.onStart();
			taskEventListener.logMsg(this.toDb.getListName(), "Starting copy of "+this.itemsToCopy.size()+" items from "+this.fromList.getListName()+"...");
			taskEventListener.beginTask("Fetching media", 100);
			
			/*
			 * TODO rewrite this using a trans-clone?
			 */
			
			for (T item : this.itemsToCopy) {
	    		IMixedMediaItem newItem = new MixedMediaItem(item.getFilepath());
	    		newItem.setFromMediaItem(item);
	    		
	    		newItem.setFilepath("TODO insert file path here!"); // FIXME TODO
	    		File localFile = new File(newItem.getFilepath());
	    		
	    		this.fromList.copyItemFile(item, localFile.getParentFile());
	    		
	    		// TODO this next 3 methods should really be combined into a single method in MediaItemDb.
	    		this.toDb.addFile(localFile);
	    		this.toDb.persistTrackData(newItem);
	    		this.toDb.setDirtyState(DirtyState.DIRTY); // just to trigger change events.
			}
			
			if (taskEventListener.isCanceled()) {
				taskEventListener.logMsg(this.toDb.getListName(), "Task was canceled desu~.");
				ret = new TaskResult(TaskOutcome.CANCELED);
			}
			else {
				ret = new TaskResult(TaskOutcome.SUCCESS);
			}
			
		}
		catch (Throwable t) {
			ret = new TaskResult(TaskOutcome.FAILED, "Throwable while fetching media.", t);
		}
		
		taskEventListener.done();
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
