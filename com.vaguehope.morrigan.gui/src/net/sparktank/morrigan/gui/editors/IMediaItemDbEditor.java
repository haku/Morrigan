package net.sparktank.morrigan.gui.editors;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;

public interface IMediaItemDbEditor<H extends IMediaItemDb<H,S,T>, S extends IMediaItemStorageLayer<T>, T extends IMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// TODO add more methods?
	
	public IMediaItemDb<H,S,T> getMediaList ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
