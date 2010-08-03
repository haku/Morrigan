package net.sparktank.morrigan.model.pictures;

import net.sparktank.morrigan.model.MediaItemList;

public abstract class MediaPictureList<T extends MediaPicture> extends MediaItemList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected MediaPictureList (String listId, String listName) {
		super(listId, listName);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Update methods.  Use these for data that is to be persisted.
//	These methods are sub-classed where persistence is needed.
	
	public void setPictureWidthAndHeight (MediaPicture mp, int width, int height) {
		mp.setWidth(width);
		mp.setHeight(height);
		setDirtyState(DirtyState.METADATA);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
