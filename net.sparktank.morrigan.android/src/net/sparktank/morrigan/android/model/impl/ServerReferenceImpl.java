package net.sparktank.morrigan.android.model.impl;

import net.sparktank.morrigan.android.model.ServerReference;

public class ServerReferenceImpl implements ServerReference {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String baseUrl;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public ServerReferenceImpl (String baseUrl) {
		this.baseUrl = baseUrl;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getBaseUrl() {
		return this.baseUrl;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
