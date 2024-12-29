package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.util.Date;

import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;


public interface IMixedMediaStorageLayer extends IMediaItemStorageLayer {
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

	void setItemMediaType(IMediaItem item, MediaType newType) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for MediaTrack.

	/**
	 * Inc start count by one and set last played date.
	 * @param sfile
	 * @throws DbException
	 */
	void incTrackPlayed (IMediaItem item) throws DbException;

	void incTrackFinished (IMediaItem item) throws DbException;
	void incTrackStartCnt (IMediaItem item, long n) throws DbException;
	void setTrackStartCnt (IMediaItem item, long n) throws DbException;
	void incTrackEndCnt (IMediaItem item, long n) throws DbException;
	void setTrackEndCnt (IMediaItem item, long n) throws DbException;
	void setDateLastPlayed (IMediaItem item, Date date) throws DbException;
	void setTrackDuration (IMediaItem item, int duration) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for MediaPicture.

	void setDimensions (IMediaItem item, int width, int height) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
