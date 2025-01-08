package com.vaguehope.morrigan.model.media;


/**
 * For efficiency reasons, events may be called with null arguments
 * if object representations are not readily available at the time
 * the event is fired.
 * A null argument does not mean no event as occurred.
 */
public interface MediaListChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void eventMessage (String msg);

	void dirtyStateChanged (DirtyState oldState, DirtyState newState);

	void mediaListRead ();
	void mediaItemsAdded (MediaItem... items);
	void mediaItemsRemoved (MediaItem... items);
	void mediaItemsUpdated (MediaItem... items);
	void mediaItemsForceReadRequired (MediaItem... items);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
