package net.sparktank.morrigan.android.model;

public interface MlistStateChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * This method will always be called in the UI thread.
	 */
	public void onMlistStateChange (MlistState newState);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
