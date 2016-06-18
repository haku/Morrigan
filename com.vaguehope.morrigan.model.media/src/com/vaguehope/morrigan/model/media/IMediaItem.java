package com.vaguehope.morrigan.model.media;

import java.math.BigInteger;
import java.util.Date;

import com.vaguehope.morrigan.model.db.IDbItem;



public interface IMediaItem extends IDbItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * File size in bytes.
	 */
	long getFileSize ();

	String getFilepath ();
	boolean setFilepath (String filePath);

	/**
	 * May return null.
	 */
	String getMimeType ();

	Date getDateAdded();
	boolean setDateAdded(Date dateAdded);

	BigInteger getHashcode();
	boolean setHashcode(BigInteger hashcode);

	Date getDateLastModified();
	boolean setDateLastModified(Date lastModified);

	boolean isEnabled();
	/**
	 * May return null.
	 */
	Date enabledLastModified();
	boolean setEnabled(boolean enabled);
	boolean setEnabled(boolean enabled, Date lastModified);

	boolean isMissing();
	boolean setMissing(boolean missing);

	String getRemoteLocation();
	boolean setRemoteLocation(String remoteLocation);

	/**
	 * May return null.
	 */
	String getRemoteId ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Reset item back to its default state.
	 * This may not reset fixed properties such as filename.
	 */
	void reset ();

	boolean setFromMediaItem (IMediaItem mt);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	String getTitle ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
