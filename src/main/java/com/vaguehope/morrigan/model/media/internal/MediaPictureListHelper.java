package com.vaguehope.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;

public final class MediaPictureListHelper {

	private MediaPictureListHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void setPictureWidthAndHeight (IMediaItemList mtl, MediaItem mp, int width, int height) {
		mp.setWidth(width);
		mp.setHeight(height);
		mtl.getChangeEventCaller().mediaItemsUpdated(mp);
		mtl.setDirtyState(DirtyState.METADATA);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
