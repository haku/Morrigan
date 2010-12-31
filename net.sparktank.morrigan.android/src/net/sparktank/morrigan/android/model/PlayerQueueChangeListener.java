package net.sparktank.morrigan.android.model;

public interface PlayerQueueChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * This method will always be called in the UI thread.
	 */
	public void onPlayerQueueChange (PlayerQueue newQueue);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
