package net.sparktank.morrigan.model.media;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;

public interface IAbstractMixedMediaDb<H extends IAbstractMixedMediaDb<H>>
		extends IMixedMediaList<IMixedMediaItem>, IMediaTrackDb<H, IMixedMediaStorageLayer<IMixedMediaItem>, IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setDefaultMediaType (MediaType mediaType) throws MorriganException;
	public void setDefaultMediaType (MediaType mediaType, boolean saveToDb) throws MorriganException;
	public MediaType getDefaultMediaType ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
