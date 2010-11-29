package net.sparktank.morrigan.model.media;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;


public interface IMixedMediaList<T extends IMixedMediaItem> extends IMediaItemList<T>, IMediaTrackList<T>, IMediaPictureList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setItemMediaType (IMixedMediaItem item, MediaType newType) throws MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
