package net.sparktank.morrigan.model.media;

import net.sparktank.morrigan.exceptions.MorriganException;

public interface IMediaPictureList <T extends IMediaPicture> extends IMediaItemList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setPictureWidthAndHeight (T item, int width, int height) throws MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
