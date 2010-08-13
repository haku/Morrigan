package net.sparktank.morrigan.model;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;

public interface IMediaItemList<T extends IMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public enum DirtyState { CLEAN, DIRTY, METADATA };
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getListId ();
	public String getListName ();
	
	public String getType ();
	public String getSerial ();
	
	public DirtyState getDirtyState ();
	public void addDirtyChangeEvent (Runnable r);
	public void removeDirtyChangeEvent (Runnable r);
	public void addChangeEvent (Runnable r);
	public void removeChangeEvent (Runnable r);
	
	public int getCount ();
	public List<T> getMediaTracks();
	
	/**
	 * This is the signal to read any source data needed.
	 * This will be called soon after the constructor and before
	 * any content is read.
	 * It may be called when no work needs doing and its
	 * up to the implemented to track this.
	 */
	public void read () throws MorriganException;
	
	public void addTrack (T track);
	public void removeMediaTrack (T track) throws MorriganException;
	
	public void setDateAdded (T track, Date date) throws MorriganException;
	public void setTrackHashCode (T track, long hashcode) throws MorriganException;
	public void setTrackDateLastModified (T track, Date date) throws MorriganException;
	public void setTrackEnabled (T track, boolean value) throws MorriganException;
	public void setTrackMissing (T track, boolean value) throws MorriganException;
	
	public void copyMediaItemFile (T mi, File targetDirectory) throws MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
