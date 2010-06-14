package net.sparktank.morrigan.model;

import java.io.File;
import java.util.Date;

import net.sparktank.morrigan.helpers.EqualHelper;

public class MediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaItem () {}
	
	public MediaItem (String filePath) {
		filepath = filePath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private String filepath = null;
	private Date dateAdded = null;
	private long startCount = 0;
	private long endCount = 0;
	private Date dateLastPlayed = null;
	private int duration = -1;
	private long hashcode = 0;
	private Date dateLastModified = null;
	private boolean enabled = true;
	private boolean missing = false;
	
	public String getFilepath () {
		return filepath;
	}
	public boolean setFilepath (String filePath) {
		if (!EqualHelper.areEqual(this.filepath, filePath)) {
			filepath = filePath;
			return true;
		}
		return false;
	}
	
	public Date getDateAdded() {
		return dateAdded;
	}
	public boolean setDateAdded(Date dateAdded) {
		if (!EqualHelper.areEqual(this.dateAdded, dateAdded)) {
			this.dateAdded = dateAdded;
			return true;
		}
		return false;
	}
	
	public long getStartCount() {
		return startCount;
	}
	public boolean setStartCount(long startCount) {
		if (this.startCount != startCount) {
			this.startCount = startCount;
			return true;
		}
		return false;
	}

	public long getEndCount() {
		return endCount;
	}
	public boolean setEndCount(long endCount) {
		if (this.endCount != endCount) {
			this.endCount = endCount;
			return true;
		}
		return false;
	}

	public Date getDateLastPlayed() {
		return dateLastPlayed;
	}
	public boolean setDateLastPlayed(Date dateLastPlayed) {
		if (!EqualHelper.areEqual(this.dateLastPlayed, dateLastPlayed)) {
			this.dateLastPlayed = dateLastPlayed;
			return true;
		}
		return false;
	}
	
	public int getDuration() {
		return duration;
	}
	public boolean setDuration(int duration) {
		if (this.duration != duration) {
			this.duration = duration;
			return true;
		}
		return false;
	}
	
	public long getHashcode() {
		return hashcode;
	}
	public boolean setHashcode(long hashcode) {
		if (this.hashcode != hashcode) {
			this.hashcode = hashcode;
			return true;
		}
		return false;
	}
	
	public Date getDateLastModified() {
		return dateLastModified;
	}
	public boolean setDateLastModified(Date lastModified) {
		if (!EqualHelper.areEqual(this.dateLastModified, lastModified)) {
			this.dateLastModified = lastModified;
			return true;
		}
		return false;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	public boolean setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			return true;
		}
		return false;
	}
	
	public boolean isMissing() {
		return missing;
	}
	public boolean setMissing(boolean missing) {
		if (this.missing != missing) {
			this.missing = missing;
			return true;
		}
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Secondary attributes.
	
	public String getTitle () {
		int x = filepath.lastIndexOf(File.separator);
		if (x>0) {
			return filepath.substring(x+1);
		} else {
			return filepath;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.
	
	public boolean setFromMediaItem (MediaItem mi) {
		boolean b = this.setFilepath(mi.getFilepath())
			|| this.setDateAdded(mi.getDateAdded())
			|| this.setStartCount(mi.getStartCount())
			|| this.setEndCount(mi.getEndCount())
			|| this.setDateLastPlayed(mi.getDateLastPlayed())
			|| this.setDuration(mi.getDuration())
			|| this.setHashcode(mi.getHashcode())
			|| this.setDateLastModified(mi.getDateLastModified())
			|| this.setEnabled(mi.isEnabled())
			|| this.setMissing(mi.isMissing());
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
