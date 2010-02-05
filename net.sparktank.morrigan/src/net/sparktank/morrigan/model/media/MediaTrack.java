package net.sparktank.morrigan.model.media;

import java.io.File;
import java.util.Date;

import net.sparktank.morrigan.helpers.EqualHelper;

public class MediaTrack {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaTrack () {}
	
	public MediaTrack (String filePath) {
		trackFilePath = filePath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private String trackFilePath = null;
	private Date dateAdded = null;
	
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
		if ( !(aThat instanceof MediaTrack) ) return false;
		MediaTrack that = (MediaTrack)aThat;
		
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
