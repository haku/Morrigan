package net.sparktank.morrigan.model.pictures;

import net.sparktank.morrigan.model.media.interfaces.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.MediaItemList;

public class MediaPictureListHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void setPictureWidthAndHeight (MediaItemList<?> mtl, MediaPicture mp, int width, int height) {
		mp.setWidth(width);
		mp.setHeight(height);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
