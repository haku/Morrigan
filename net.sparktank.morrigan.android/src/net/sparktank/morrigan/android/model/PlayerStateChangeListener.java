package net.sparktank.morrigan.android.model;

public interface PlayerStateChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * This method will always be called in the UI thread.
	 */
	public void onPlayerStateChange (PlayerState newState);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
