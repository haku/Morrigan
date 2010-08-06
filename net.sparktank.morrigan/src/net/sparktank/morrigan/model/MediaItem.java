package net.sparktank.morrigan.model;

import java.io.File;
import java.util.Date;

import net.sparktank.morrigan.helpers.EqualHelper;

/**
 * Generic media item, be it music, video, image, etc...
 */
public class MediaItem implements IDbItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaItem () {/*Empty constructor.*/}
	
	public MediaItem (String filePath) {
		this.filepath = filePath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private String filepath = null;
	private Date dateAdded = null;
	private Date dateLastPlayed = null;
	private long hashcode = 0;
	private Date dateLastModified = null;
	private boolean enabled = true;
	private boolean missing = false;
	
	public String getFilepath () {
		return this.filepath;
	}
	public boolean setFilepath (String filePath) {
		if (!EqualHelper.areEqual(this.filepath, filePath)) {
			this.filepath = filePath;
			return true;
		}
		return false;
	}
	
	public Date getDateAdded() {
		return this.dateAdded;
	}
	public boolean setDateAdded(Date dateAdded) {
		if (!EqualHelper.areEqual(this.dateAdded, dateAdded)) {
			this.dateAdded = dateAdded;
			return true;
		}
		return false;
	}
	
	public Date getDateLastPlayed() {
		return this.dateLastPlayed;
	}
	public boolean setDateLastPlayed(Date dateLastPlayed) {
		if (!EqualHelper.areEqual(this.dateLastPlayed, dateLastPlayed)) {
			this.dateLastPlayed = dateLastPlayed;
			return true;
		}
		return false;
	}
	
	public long getHashcode() {
		return this.hashcode;
	}
	public boolean setHashcode(long hashcode) {
		if (this.hashcode != hashcode) {
			this.hashcode = hashcode;
			return true;
		}
		return false;
	}
	
	public Date getDateLastModified() {
		return this.dateLastModified;
	}
	public boolean setDateLastModified(Date lastModified) {
		if (!EqualHelper.areEqual(this.dateLastModified, lastModified)) {
			this.dateLastModified = lastModified;
			return true;
		}
		return false;
	}
	
	public boolean isEnabled() {
		return this.enabled;
	}
	public boolean setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			return true;
		}
		return false;
	}
	
	public boolean isMissing() {
		return this.missing;
	}
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
	
	public String getTitle () {
		int x = this.filepath.lastIndexOf(File.separator);
		if (x>0) {
			return this.filepath.substring(x+1);
		}
		
		return this.filepath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.
	
	public boolean setFromMediaItem (MediaItem mi) {
		boolean b = this.setFilepath(mi.getFilepath())
			|| this.setDateAdded(mi.getDateAdded())
			|| this.setDateLastPlayed(mi.getDateLastPlayed())
			|| this.setHashcode(mi.getHashcode())
			|| this.setDateLastModified(mi.getDateLastModified())
			|| this.setEnabled(mi.isEnabled())
			|| this.setMissing(mi.isMissing())
			|| this.setDbRowId(mi.getDbRowId())
			|| this.setRemoteLocation(mi.getRemoteLocation());
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
