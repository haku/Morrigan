package net.sparktank.morrigan.model.media;

import com.vaguehope.morrigan.model.exceptions.MorriganException;

import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;


public interface IMixedMediaList<T extends IMixedMediaItem> extends IMediaTrackList<T>, IMediaPictureList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setItemMediaType (IMixedMediaItem item, MediaType newType) throws MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
