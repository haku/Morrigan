package com.vaguehope.morrigan.model.media;

import com.vaguehope.morrigan.model.exceptions.MorriganException;


public interface IMediaPictureList <T extends IMediaPicture> extends IMediaItemList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void setPictureWidthAndHeight (IMediaPicture item, int width, int height) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
