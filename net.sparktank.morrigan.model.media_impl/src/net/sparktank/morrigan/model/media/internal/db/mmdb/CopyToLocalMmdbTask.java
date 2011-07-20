package net.sparktank.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaItem;
import net.sparktank.morrigan.model.media.IMediaItemList;
import net.sparktank.morrigan.model.media.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tasks.TaskResult;
import net.sparktank.morrigan.model.tasks.TaskResult.TaskOutcome;
import net.sparktank.morrigan.util.ChecksumHelper;

/**
 * 
 * @param <T> the type of the source list.
 */
public class CopyToLocalMmdbTask<T extends IMediaItem> implements IMorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final IMediaItemList<T> fromList;
	private final Collection<T> itemsToCopy;
	private final ILocalMixedMediaDb toDb;
	
	public CopyToLocalMmdbTask (IMediaItemList<T> fromList, Collection<T> itemsToCopy, ILocalMixedMediaDb toDb) {
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
			
			File coDir = getCheckoutDirectory(this.toDb);
			
			/*
			 * TODO rewrite this using a trans-clone?
			 */
			
			for (T item : this.itemsToCopy) {
				File coItemDir = getCheckoutItemDirectory(coDir, item);
				File coFile = this.fromList.copyItemFile(item, coItemDir);
	    		if (!coFile.exists()) throw new FileNotFoundException("After fetching '"+item.getRemoteLocation()+"' can't find '"+coFile.getAbsolutePath()+"'.");
	    		
	    		IMixedMediaItem newItem = this.toDb.getDbLayer().getNewT(coFile.getAbsolutePath());
	    		newItem.setFromMediaItem(item);
	    		
	    		// TODO FIXME re-write remote path with URL we fetched it from?  Perhaps this should be returned from copyItemFile()?
	    		
	    		// TODO these next few methods should really be combined into a single method in MediaItemDb.
	    		if (this.toDb.getDbLayer().hasFile(coFile)) {
	    			this.toDb.getDbLayer().removeFile(coFile.getAbsolutePath());
	    		}
	    		IMixedMediaItem addedItem = this.toDb.addFile(coFile);
	    		addedItem.setFromMediaItem(newItem);
	    		this.toDb.persistTrackData(addedItem);
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
	
	// TODO extract this to config?
	static private File getCheckoutDirectory (ILocalMixedMediaDb db) {
		String configDir = Config.getConfigDir();
		
		File coDir = new File(configDir, "checkout");
		if (!coDir.exists()) {
			if (!coDir.mkdir()) {
				throw new RuntimeException("Failed to mkdir '"+coDir.getAbsolutePath()+"'.");
			}
		}
		
		File dbCoDir = new File(coDir, db.getListName());
		if (!dbCoDir.exists()) {
			if (!dbCoDir.mkdir()) {
				throw new RuntimeException("Failed to mkdir '"+dbCoDir.getAbsolutePath()+"'.");
			}
		}
		
		return dbCoDir;
	}
	
	static private File getCheckoutItemDirectory (File coDir, IMediaItem item) {
		String srcPath = item.getRemoteLocation();
		
		File dir = new File(coDir, ChecksumHelper.md5String(srcPath));
		if (!dir.exists()) {
			if (!dir.mkdir()) {
				throw new RuntimeException("Failed to mkdir '"+dir.getAbsolutePath()+"'.");
			}
		}
		
		return dir;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
