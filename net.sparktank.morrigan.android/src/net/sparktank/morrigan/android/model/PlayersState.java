package net.sparktank.morrigan.android.model;

import java.util.List;

public interface PlayersState extends ArtifactList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public List<? extends PlayerState> getPlayersState ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
