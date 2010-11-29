package net.sparktank.morrigan.model.media.interfaces;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.model.db.IDbColumn;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem.MediaType;
import net.sparktank.sqlitewrapper.DbException;

public interface IMixedMediaStorageLayer<T extends IMixedMediaItem> extends IMediaItemStorageLayer<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Read methods.
	
	/**
	 * Set the default media type for the untyped getters to return.
	 */
	public void setDefaultMediaType (MediaType mediaType);
	public MediaType getDefaultMediaType();
	
	public List<IMixedMediaItem> getAllMedia (MediaType mediaType, IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<IMixedMediaItem> updateListOfAllMedia (MediaType mediaType, List<IMixedMediaItem> list, IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<IMixedMediaItem> updateListOfAllMedia (MediaType mediaType, List<IMixedMediaItem> list, IDbColumn sort, SortDirection direction, boolean hideMissing, String search, String searchEsc) throws DbException;
	public List<IMixedMediaItem> simpleSearchMedia (MediaType mediaType, String term, String esc, int maxResults) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for generic MediaItem.
	
	/**
	 * 
	 * @param file
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	public boolean addFile (MediaType mediaType, File file) throws DbException;
	
	/**
	 * 
	 * @param filepath
	 * @param lastModified
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	public boolean addFile (MediaType mediaType, String filepath, long lastModified) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for MixedMediaItem.
	
	public void setItemMediaType(String sfile, MediaType newType) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for MediaTrack.
	
	/**
	 * Inc start count by one and set last played date.
	 * @param sfile
	 * @throws DbException
	 */
	public void incTrackPlayed (String sfile) throws DbException;
	
	public void incTrackFinished (String sfile) throws DbException;
	public void incTrackStartCnt (String sfile, long n) throws DbException;
	public void setTrackStartCnt (String sfile, long n) throws DbException;
	public void incTrackEndCnt (String sfile, long n) throws DbException;
	public void setTrackEndCnt (String sfile, long n) throws DbException;
	public void setDateLastPlayed (String sfile, Date date) throws DbException;
	public void setTrackDuration (String sfile, int duration) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for MediaPicture.
	
	public void setDimensions (String sfile, int width, int height) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
