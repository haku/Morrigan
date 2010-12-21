package net.sparktank.morrigan.android.model.impl;

import net.sparktank.morrigan.android.model.PlayerReference;
import net.sparktank.morrigan.android.model.ServerReference;

public class PlayerReferenceImpl implements PlayerReference {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String baseUrl;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PlayerReferenceImpl (ServerReference serverReference, int playerId) {
		this.baseUrl = serverReference.getBaseUrl() + "/players/" + playerId; // TODO extract constant.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getBaseUrl() {
		return this.baseUrl;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
