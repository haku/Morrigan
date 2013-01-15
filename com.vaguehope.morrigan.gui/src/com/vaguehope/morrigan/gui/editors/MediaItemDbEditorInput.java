package com.vaguehope.morrigan.gui.editors;


import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;

/**
 * Subclass of MediaItemListEditorInput to allow saving of sort configuration.
 */
public class MediaItemDbEditorInput extends MediaItemListEditorInput<IMediaItemDb<? extends IMediaItemStorageLayer<? extends IMediaItem>, ? extends IMediaItem>> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MediaItemDbEditorInput(IMediaItemDb<? extends IMediaItemStorageLayer<? extends IMediaItem>, ? extends IMediaItem> mediaList) {
		super(mediaList);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
