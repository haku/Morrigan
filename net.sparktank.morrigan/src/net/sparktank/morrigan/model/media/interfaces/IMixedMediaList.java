package net.sparktank.morrigan.model.media.interfaces;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem.MediaType;



public interface IMixedMediaList<T extends IMixedMediaItem> extends IMediaItemList<T>, IMediaTrackList<T>, IMediaPictureList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setItemMediaType (IMixedMediaItem item, MediaType newType) throws MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
