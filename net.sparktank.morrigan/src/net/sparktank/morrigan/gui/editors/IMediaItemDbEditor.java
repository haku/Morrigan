package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemStorageLayer;

public interface IMediaItemDbEditor<S extends IMediaItemStorageLayer<T>, T extends IMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// TODO add more methods?
	
	public IMediaItemDb<S,T> getMediaList ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
