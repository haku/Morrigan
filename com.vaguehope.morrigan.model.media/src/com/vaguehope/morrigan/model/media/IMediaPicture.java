package com.vaguehope.morrigan.model.media;

public interface IMediaPicture extends IMediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	boolean isPicture ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	int getWidth();
	boolean setWidth(int width);

	int getHeight();
	boolean setHeight(int height);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	boolean setFromMediaPicture (IMediaPicture mp);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
