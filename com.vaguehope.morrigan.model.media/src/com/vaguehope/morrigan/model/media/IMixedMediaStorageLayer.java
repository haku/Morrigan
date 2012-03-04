package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.util.Date;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.sqlitewrapper.DbException;


public interface IMixedMediaStorageLayer<T extends IMixedMediaItem> extends IMediaItemStorageLayer<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Read methods.

	/**
	 * Set the default media type for the untyped getters to return.
	 */
	void setDefaultMediaType (MediaType mediaType);
	MediaType getDefaultMediaType();

	List<IMixedMediaItem> getMedia (MediaType mediaType, IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	List<IMixedMediaItem> getMedia (MediaType mediaType, IDbColumn sort, SortDirection direction, boolean hideMissing, String search, String searchEsc) throws DbException;
	List<IMixedMediaItem> simpleSearchMedia (MediaType mediaType, String term, String esc, int maxResults) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for generic MediaItem.

	/**
	 * 
	 * @param file
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	boolean addFile (MediaType mediaType, File file) throws DbException;

	/**
	 * 
	 * @param filepath
	 * @param lastModified
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	boolean addFile (MediaType mediaType, String filepath, long lastModified) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for MixedMediaItem.

	void setItemMediaType(String sfile, MediaType newType) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for MediaTrack.

	/**
	 * Inc start count by one and set last played date.
	 * @param sfile
	 * @throws DbException
	 */
	void incTrackPlayed (String sfile) throws DbException;

	void incTrackFinished (String sfile) throws DbException;
	void incTrackStartCnt (String sfile, long n) throws DbException;
	void setTrackStartCnt (String sfile, long n) throws DbException;
	void incTrackEndCnt (String sfile, long n) throws DbException;
	void setTrackEndCnt (String sfile, long n) throws DbException;
	void setDateLastPlayed (String sfile, Date date) throws DbException;
	void setTrackDuration (String sfile, int duration) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for MediaPicture.

	void setDimensions (String sfile, int width, int height) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
