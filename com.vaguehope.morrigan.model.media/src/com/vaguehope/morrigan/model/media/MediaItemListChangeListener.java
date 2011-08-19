package com.vaguehope.morrigan.model.media;


/**
 * For efficiency reasons, events may be called with null arguments
 * if object representations are not readily available at the time
 * the event is fired.
 * A null argument does not mean no event as occurred.
 */
public interface MediaItemListChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void dirtyStateChanged (DirtyState oldState, DirtyState newState);
	
	public void mediaListRead ();
	public void mediaItemsAdded (IMediaItem... items);
	public void mediaItemsRemoved (IMediaItem... items);
	public void mediaItemsUpdated (IMediaItem... items);
	public void mediaItemsForceReadRequired (IMediaItem... items);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
