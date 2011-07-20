package net.sparktank.morrigan.model.media.internal;

import java.io.File;
import java.math.BigInteger;
import java.util.Date;

import net.sparktank.morrigan.model.db.IDbItem;
import net.sparktank.morrigan.model.helper.EqualHelper;
import net.sparktank.morrigan.model.media.IMediaItem;

/**
 * Generic media item, be it music, video, image, etc...
 */
public abstract class MediaItem implements IMediaItem, IDbItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaItem (String filePath) {
		setFilepath(filePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private String filepath = null;
	private Date dateAdded = null;
	private BigInteger hashcode = BigInteger.ZERO;
	private Date dateLastModified = null;
	private boolean enabled = true;
	private boolean missing = false;
	
	@Override
	public String getFilepath () {
		return this.filepath;
	}
	@Override
	public boolean setFilepath (String filepath) {
		if (this.filepath == null && filepath != null) {
			return setFilepathIfNotSet(filepath);
		}
		else if (EqualHelper.areEqual(this.filepath, filepath)) {
			return false;
		}
		else {
			throw new IllegalStateException("filepath can not be modified once set.  Current='"+this.filepath+"' proposed='"+filepath+"'.");
		}
	}
	
	/**
	 * This will silently fail if filepath is already set.
	 */
	private boolean setFilepathIfNotSet (String filepath) {
		if (this.filepath == null && !EqualHelper.areEqual(this.filepath, filepath)) {
			this.filepath = filepath;
			updateTitle();
			return true;
		}
		return false;
	}
	
	@Override
	public Date getDateAdded() {
		return this.dateAdded;
	}
	@Override
	public boolean setDateAdded(Date dateAdded) {
		if (!EqualHelper.areEqual(this.dateAdded, dateAdded)) {
			this.dateAdded = dateAdded;
			return true;
		}
		return false;
	}
	
	@Override
	public BigInteger getHashcode() {
		return this.hashcode;
	}
	@Override
	public boolean setHashcode(BigInteger hashcode) {
		if (!this.hashcode.equals(hashcode)) {
			this.hashcode = hashcode;
			return true;
		}
		return false;
	}
	
	@Override
	public Date getDateLastModified() {
		return this.dateLastModified;
	}
	@Override
	public boolean setDateLastModified(Date lastModified) {
		if (!EqualHelper.areEqual(this.dateLastModified, lastModified)) {
			this.dateLastModified = lastModified;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isEnabled() {
		return this.enabled;
	}
	@Override
	public boolean setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isMissing() {
		return this.missing;
	}
	@Override
	public boolean setMissing(boolean missing) {
		if (this.missing != missing) {
			this.missing = missing;
			return true;
		}
		return false;
	}
	
//	-  -  -  -  -  -  -  -
	
	private long dbRowId;
	private String remoteLocation;
	
	@Override
	public long getDbRowId() {
		return this.dbRowId;
	}
	@Override
	public boolean setDbRowId(long dbRowId) {
		/* Sqlite ROWID starts at 1, so if something tries to set it
		 * less than this, don't let them clear it.
		 * This is most likely when fetching a remote list over HTTP.
		 */
		if (dbRowId > 0 && this.dbRowId != dbRowId) {
			this.dbRowId = dbRowId;
			return true;
		}
		return false;
	}
	
	@Override
	public String getRemoteLocation() {
		return this.remoteLocation;
	}
	@Override
	public boolean setRemoteLocation(String remoteLocation) {
		if (!EqualHelper.areEqual(this.remoteLocation, remoteLocation)) {
			this.remoteLocation = remoteLocation;
			return true;
		}
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Secondary attributes.
	
	private String title = null;
	
	private void updateTitle () {
		int x = this.filepath.lastIndexOf(File.separator);
		if (x>0) {
			this.title = this.filepath.substring(x+1);
		}
		else {
			this.title = this.filepath;
		}
	}
	
	@Override
	public String getTitle () {
		return this.title;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.
	
	@Override
	public boolean setFromMediaItem (IMediaItem mi) {
		boolean b =
    		  this.setFilepathIfNotSet(mi.getFilepath()) // Do not set it if it is already set.
    		| this.setDateAdded(mi.getDateAdded())
    		| this.setHashcode(mi.getHashcode())
    		| this.setDateLastModified(mi.getDateLastModified())
    		| this.setEnabled(mi.isEnabled())
    		| this.setMissing(mi.isMissing())
    		| this.setDbRowId(mi.getDbRowId())
    		| this.setRemoteLocation(mi.getRemoteLocation());
    	return b;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public boolean equals(Object aThat) {
		if ( aThat == null ) return false;
		if ( this == aThat ) return true;
		if ( !(aThat instanceof MediaItem) ) return false;
		MediaItem that = (MediaItem)aThat;
		
		return EqualHelper.areEqual(getFilepath(), that.getFilepath());
	}
	
	@Override
	public int hashCode() {
		return getFilepath().hashCode();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString () {
		return getTitle();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
