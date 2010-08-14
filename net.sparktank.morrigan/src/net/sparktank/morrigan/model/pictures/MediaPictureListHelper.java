package net.sparktank.morrigan.model.pictures;

import net.sparktank.morrigan.model.media.interfaces.IMediaItemList;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.media.interfaces.IMediaPicture;

public class MediaPictureListHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void setPictureWidthAndHeight (IMediaItemList<?> mtl, IMediaPicture mp, int width, int height) {
		mp.setWidth(width);
		mp.setHeight(height);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
