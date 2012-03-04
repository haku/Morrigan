package com.vaguehope.morrigan.model.media;


/**
 * For efficiency reasons, events may be called with null arguments
 * if object representations are not readily available at the time
 * the event is fired.
 * A null argument does not mean no event as occurred.
 */
public interface MediaItemListChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void eventMessage (String msg);

	void dirtyStateChanged (DirtyState oldState, DirtyState newState);

	void mediaListRead ();
	void mediaItemsAdded (IMediaItem... items);
	void mediaItemsRemoved (IMediaItem... items);
	void mediaItemsUpdated (IMediaItem... items);
	void mediaItemsForceReadRequired (IMediaItem... items);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
