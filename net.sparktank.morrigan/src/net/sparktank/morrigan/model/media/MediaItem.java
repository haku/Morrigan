package net.sparktank.morrigan.model.media;

import java.io.File;
import java.util.Date;

import net.sparktank.morrigan.helpers.EqualHelper;

public class MediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaItem () {}
	
	public MediaItem (String filePath) {
		trackFilePath = filePath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private String trackFilePath = null;
	private Date dateAdded = null;
	private long startCount = 0;
	private long endCount = 0;
	private Date dateLastPlayed = null;
	private int duration = -1;
	private long hashcode = 0;
	private boolean enabled = true;
	private boolean missing = false;
	
	public String getFilepath () {
		return trackFilePath;
	}
	public void setfilepath (String filePath) {
		trackFilePath = filePath;
	}
	
	public Date getDateAdded() {
		return dateAdded;
	}
	public void setDateAdded(Date dateAdded) {
		this.dateAdded = dateAdded;
	}
	
	public long getStartCount() {
		return startCount;
	}
	public void setStartCount(long startCount) {
		this.startCount = startCount;
	}

	public long getEndCount() {
		return endCount;
	}
	public void setEndCount(long endCount) {
		this.endCount = endCount;
	}

	public Date getDateLastPlayed() {
		return dateLastPlayed;
	}
	public void setDateLastPlayed(Date dateLastPlayed) {
		this.dateLastPlayed = dateLastPlayed;
	}
	
	public int getDuration() {
		return duration;
	}
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	public long getHashcode() {
		return hashcode;
	}
	public void setHashcode(long hashcode) {
		this.hashcode = hashcode;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public boolean isMissing() {
		return missing;
	}
	public void setMissing(boolean missing) {
		this.missing = missing;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Secondary attributes.
	
	public String getTitle () {
		int x = trackFilePath.lastIndexOf(File.separator);
		if (x>0) {
			return trackFilePath.substring(x+1);
		} else {
			return trackFilePath;
		}
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
