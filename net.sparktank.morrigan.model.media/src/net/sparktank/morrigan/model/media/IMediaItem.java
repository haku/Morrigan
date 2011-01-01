package net.sparktank.morrigan.model.media;

import java.math.BigInteger;
import java.util.Date;

import net.sparktank.morrigan.model.db.IDbItem;


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
	
	public boolean setFromMediaItem (IMediaItem mt);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getTitle ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
