package com.vaguehope.morrigan.model.media;

import java.math.BigInteger;
import java.util.Date;

import com.vaguehope.morrigan.model.db.IDbItem;



public interface IMediaItem extends IDbItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getFilepath ();
	public boolean setFilepath (String filePath);
	
	public Date getDateAdded();
	public boolean setDateAdded(Date dateAdded);
	
	public BigInteger getHashcode();
	public boolean setHashcode(BigInteger hashcode);
	
	public Date getDateLastModified();
	public boolean setDateLastModified(Date lastModified);
	
	public boolean isEnabled();
	public boolean setEnabled(boolean enabled);
	
	public boolean isMissing();
	public boolean setMissing(boolean missing);
	
	public String getRemoteLocation();
	public boolean setRemoteLocation(String remoteLocation);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Reset item back to its default state.
	 * This may not reset fixed properties such as filename.
	 */
	public void reset ();
	
	public boolean setFromMediaItem (IMediaItem mt);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getTitle ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
