package net.sparktank.morrigan.android.model.impl;

import net.sparktank.morrigan.android.model.PlayerArtifact;

public class PlayerArtifactImpl implements PlayerArtifact {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final int id;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PlayerArtifactImpl (int id) {
		this.id = id;
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	PlayerArtifact methods.
	
	@Override
	public int getId () {
		return this.id;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Artifact methods.
	
	@Override
	public String getTitle() {
		return "p" + this.id;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
