package net.sparktank.morrigan.android.model;

public interface PlayerState {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public int getId ();
	
	public PlayState getPlayState ();
	public int getPlayOrder (); // TODO replace with enum.
	public int getPlayerPosition ();
	
	public String getListTitle ();
	public String getListId ();
	
	public String getTrackTitle ();
	public String getTrckFile ();
	public int getTrackDuration ();
	
	public int getQueueLength ();
	// TODO queueduration
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -	
}
