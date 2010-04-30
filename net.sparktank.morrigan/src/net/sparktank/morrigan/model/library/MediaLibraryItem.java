package net.sparktank.morrigan.model.library;

import net.sparktank.morrigan.model.MediaItem;

public class MediaLibraryItem extends MediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private long dbRowId;
	
	public long getDbRowId() {
		return dbRowId;
	}
	public void setDbRowId(long dbRowId) {
		this.dbRowId = dbRowId;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
