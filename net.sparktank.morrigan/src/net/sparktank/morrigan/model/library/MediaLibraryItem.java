package net.sparktank.morrigan.model.library;

import net.sparktank.morrigan.helpers.EqualHelper;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaTrack;

// FIXME rename to MediaLibraryTrack
public class MediaLibraryItem extends MediaTrack {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaLibraryItem() {
		super();
	}
	
	public MediaLibraryItem(String filepath) {
		super(filepath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private long dbRowId;
	private String remoteLocation;
	
	public long getDbRowId() {
		return dbRowId;
	}
	public boolean setDbRowId(long dbRowId) {
		if (this.dbRowId != dbRowId) {
			this.dbRowId = dbRowId;
			return true;
		}
		return false;
	}
	
	public String getRemoteLocation() {
		return remoteLocation;
	}
	public boolean setRemoteLocation(String remoteLocation) {
		if (!EqualHelper.areEqual(this.remoteLocation, remoteLocation)) {
			this.remoteLocation = remoteLocation;
			return true;
		}
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.
	
	@Override
	public boolean setFromMediaItem(MediaItem mi) {
		boolean setFromMediaItem = super.setFromMediaItem(mi);
		
		if (mi instanceof MediaLibraryItem) {
			MediaLibraryItem mli = (MediaLibraryItem) mi;
			
			boolean b = this.setDbRowId(mli.getDbRowId())
				|| this.setRemoteLocation(mli.getRemoteLocation());
			
			return b || setFromMediaItem;
		}
		else {
			return setFromMediaItem;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
