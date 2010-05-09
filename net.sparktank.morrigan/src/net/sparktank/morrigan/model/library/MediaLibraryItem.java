package net.sparktank.morrigan.model.library;

import net.sparktank.morrigan.model.MediaItem;

public class MediaLibraryItem extends MediaItem {
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
		if (this.remoteLocation == null || !this.remoteLocation.equals(remoteLocation)) {
			this.remoteLocation = remoteLocation;
			return true;
		}
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public boolean setFromMediaItem(MediaItem mi) {
		if (mi instanceof MediaLibraryItem) {
			MediaLibraryItem mli = (MediaLibraryItem) mi;
			boolean b = this.setDbRowId(mli.getDbRowId())
				|| this.setRemoteLocation(mli.getRemoteLocation());
			return b || super.setFromMediaItem(mi);
			
		} else {
			return super.setFromMediaItem(mi);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
