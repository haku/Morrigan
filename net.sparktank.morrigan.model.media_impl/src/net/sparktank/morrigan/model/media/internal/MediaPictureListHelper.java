package net.sparktank.morrigan.model.media.internal;

import net.sparktank.morrigan.model.media.IMediaItemList;
import net.sparktank.morrigan.model.media.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.media.IMediaPicture;

public class MediaPictureListHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void setPictureWidthAndHeight (IMediaItemList<?> mtl, IMediaPicture mp, int width, int height) {
		mp.setWidth(width);
		mp.setHeight(height);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
