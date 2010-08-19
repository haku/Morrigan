package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemStorageLayer;

import org.eclipse.ui.IMemento;

/**
 * Subclass of MediaItemListEditorInput to allow saving of sort configuration.
 */
public class MediaItemDbEditorInput extends MediaItemListEditorInput<IMediaItemDb<?, ? extends IMediaItemStorageLayer<? extends IMediaItem>, ? extends IMediaItem>> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaItemDbEditorInput(IMediaItemDb<?, ? extends IMediaItemStorageLayer<? extends IMediaItem>, ? extends IMediaItem> mediaList) {
		super(mediaList);
	}
	
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
