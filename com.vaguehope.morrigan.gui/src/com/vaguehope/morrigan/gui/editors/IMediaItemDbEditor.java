package com.vaguehope.morrigan.gui.editors;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;

public interface IMediaItemDbEditor<S extends IMediaItemStorageLayer<T>, T extends IMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// TODO add more methods?
	
	public IMediaItemDb<S,T> getMediaList ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
