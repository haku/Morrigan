package net.sparktank.morrigan.model.media;

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
