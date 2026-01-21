package morrigan.model.media.internal;

import morrigan.model.media.DirtyState;
import morrigan.model.media.MediaItem;
import morrigan.model.media.MediaList;

public final class MediaPictureListHelper {

	private MediaPictureListHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void setPictureWidthAndHeight (MediaList mtl, MediaItem mp, int width, int height) {
		mp.setWidth(width);
		mp.setHeight(height);
		mtl.getChangeEventCaller().mediaItemsUpdated(mp);
		mtl.setDirtyState(DirtyState.METADATA);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
