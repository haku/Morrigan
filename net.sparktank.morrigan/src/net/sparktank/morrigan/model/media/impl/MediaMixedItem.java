package net.sparktank.morrigan.model.media.impl;

import java.io.File;
import java.util.Date;

import net.sparktank.morrigan.helpers.EqualHelper;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaPicture;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;

public class MediaMixedItem implements IMixedMediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaMixedItem () {/*Empty constructor.*/}
	
	public MediaMixedItem (String filePath) {
		this.filepath = filePath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
//	-  -  -  -  -  -  -  -  -
//	IMediaItem
	
	private String filepath = null;
	private Date dateAdded = null;
	private long hashcode = 0;
	private Date dateLastModified = null;
	private boolean enabled = true;
	private boolean missing = false;
	
	@Override
	public String getFilepath () {
		return this.filepath;
	}
	@Override
	public boolean setFilepath (String filePath) {
		if (!EqualHelper.areEqual(this.filepath, filePath)) {
			this.filepath = filePath;
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
	public long getHashcode() {
		return this.hashcode;
	}
	@Override
	public boolean setHashcode(long hashcode) {
		if (this.hashcode != hashcode) {
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
	
//	-  -  -  -  -  -  -  -  -
//	IDbItem
	
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
	
//	-  -  -  -  -  -  -  -  -
//	IMediaTrack.
	
	private int duration;
	private long startCount;
	private long endCount;
	private Date dateLastPlayed;
	
	@Override
	public int getDuration() {
		return this.duration;
	}
	@Override
	public boolean setDuration(int duration) {
		if (this.duration != duration) {
			this.duration = duration;
			return true;
		}
		return false;
	}
	
	@Override
	public long getStartCount() {
		return this.startCount;
	}
	@Override
	public boolean setStartCount(long startCount) {
		if (this.startCount != startCount) {
			this.startCount = startCount;
			return true;
		}
		return false;
	}

	@Override
	public long getEndCount() {
		return this.endCount;
	}
	@Override
	public boolean setEndCount(long endCount) {
		if (this.endCount != endCount) {
			this.endCount = endCount;
			return true;
		}
		return false;
	}
	
	@Override
	public Date getDateLastPlayed() {
		return this.dateLastPlayed;
	}
	@Override
	public boolean setDateLastPlayed(Date dateLastPlayed) {
		if (!EqualHelper.areEqual(this.dateLastPlayed, dateLastPlayed)) {
			this.dateLastPlayed = dateLastPlayed;
			return true;
		}
		return false;
	}
	
//	-  -  -  -  -  -  -  -  -
//	IMediaPicture.
	
	private int width;
	private int height;
	
	@Override
	public int getWidth() {
		return this.width;
	}
	@Override
	public boolean setWidth(int width) {
		if (this.width != width) {
			this.width = width;
			return true;
		}
		return false;
	}
	
	@Override
	public int getHeight() {
		return this.height;
	}
	@Override
	public boolean setHeight(int height) {
		if (this.height != height) {
			this.height = height;
			return true;
		}
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Secondary attributes.
	
	@Override
	public String getTitle () {
		int x = this.filepath.lastIndexOf(File.separator);
		if (x>0) {
			return this.filepath.substring(x+1);
		}
		
		return this.filepath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.
	
	@Override
	public boolean setFromMediaItem (IMediaItem mt) {
		boolean b =
			  this.setFilepath(mt.getFilepath())
			| this.setDateAdded(mt.getDateAdded())
			| this.setHashcode(mt.getHashcode())
			| this.setDateLastModified(mt.getDateLastModified())
			| this.setEnabled(mt.isEnabled())
			| this.setMissing(mt.isMissing())
			| this.setDbRowId(mt.getDbRowId())
			| this.setRemoteLocation(mt.getRemoteLocation());
		return b;
	}
	
	private boolean _setFromMediaTrack (IMediaTrack mt) {
		boolean b =
			  this.setDuration(mt.getDuration())
			| this.setStartCount(mt.getStartCount())
			| this.setEndCount(mt.getEndCount())
			| this.setDateLastPlayed(mt.getDateLastPlayed());
		return b;
	}
	
	@Override
	public boolean setFromMediaTrack(IMediaTrack mt) {
		boolean b =
			  this.setFromMediaItem(mt)
			| _setFromMediaTrack(mt);
		return b;
	}
	
	private boolean _setFromMediaPicture (IMediaPicture mp) {
		boolean b =
			  this.setWidth(mp.getWidth())
			| this.setHeight(mp.getHeight());
		return b;
	}
	
	@Override
	public boolean setFromMediaPicture (IMediaPicture mp) {
		boolean b =
			  this.setFromMediaItem(mp)
			| _setFromMediaPicture(mp);
		return b;
	}
	
	@Override
	public boolean setFromMediaMixedItem (IMixedMediaItem mmi) {
		boolean b =
			  this.setFromMediaItem(mmi)
			| _setFromMediaTrack(mmi)
			| _setFromMediaPicture(mmi)
			;
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
