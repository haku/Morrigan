package com.vaguehope.morrigan.model.media;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;

public interface IMixedMediaList<T extends IMixedMediaItem> extends IMediaTrackList<T>, IMediaPictureList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void setItemMediaType (IMixedMediaItem item, MediaType newType) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
