package net.sparktank.morrigan.android.model;

public interface MlistStateListChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * This method will always be called in the UI thread.
	 */
	public void onMlistsChange (MlistStateList mlistsState);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
