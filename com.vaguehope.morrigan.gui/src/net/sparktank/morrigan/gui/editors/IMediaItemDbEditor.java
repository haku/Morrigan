package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.model.media.IMediaItem;
import net.sparktank.morrigan.model.media.IMediaItemDb;
import net.sparktank.morrigan.model.media.IMediaItemStorageLayer;

public interface IMediaItemDbEditor<H extends IMediaItemDb<H,S,T>, S extends IMediaItemStorageLayer<T>, T extends IMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// TODO add more methods?
	
	public IMediaItemDb<H,S,T> getMediaList ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
