package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import com.vaguehope.morrigan.model.exceptions.MorriganException;


public interface IMediaItemList<T extends IMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void dispose ();
	
	public String getListId ();
	public String getListName ();
	
	public String getType ();
	public String getSerial (); // TODO rename to something more helpful?
	
	public DirtyState getDirtyState ();
	public void setDirtyState (DirtyState state);
	
	/**
	 * A change event will occur every time the state might have changed.
	 */
	public void addChangeEventListener (MediaItemListChangeListener listener);
	public void removeChangeEventListener (MediaItemListChangeListener listener);
	public MediaItemListChangeListener getChangeEventCaller ();
	
	public int getCount ();
	public List<T> getMediaItems();
	
	/**
	 * This is the signal to read any source data needed.
	 * This will be called soon after the constructor and before
	 * any content is read.
	 * It may be called when no work needs doing and its
	 * up to the implemented to track this.
	 */
	public void read () throws MorriganException;
	public void forceRead () throws MorriganException;
	public long getDurationOfLastRead();
	
	public void addItem (T item);
	public void removeItem (T item) throws MorriganException;
	
	public void setItemDateAdded (T item, Date date) throws MorriganException;
	public void setItemHashCode (T item, BigInteger hashcode) throws MorriganException;
	public void setItemDateLastModified (T item, Date date) throws MorriganException;
	public void setItemEnabled (T item, boolean value) throws MorriganException;
	public void setItemMissing (T item, boolean value) throws MorriganException;
	public void setRemoteLocation (T track, String remoteLocation) throws MorriganException;
	public void persistTrackData (T track) throws MorriganException;
	
	/**
	 * This will flush the OutputStream.
	 * This will not close the output stream.
	 */
	public void copyItemFile (T item, OutputStream os) throws MorriganException;
	public File copyItemFile (T item, File targetDirectory) throws MorriganException;
	
	// TODO rename to getItemByFilePath().
	public T findItemByFilePath (String path);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
