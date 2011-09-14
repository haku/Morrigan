package com.vaguehope.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.IMediaPicture;

public class MediaPictureListHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void setPictureWidthAndHeight (IMediaItemList<?> mtl, IMediaPicture mp, int width, int height) {
		mp.setWidth(width);
		mp.setHeight(height);
		mtl.getChangeEventCaller().mediaItemsUpdated(mp);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
