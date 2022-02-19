package com.vaguehope.morrigan.server.model;

import java.net.URI;

public class RemoteHostDetails {

	private final URI uri;
	private final String pass;

	public RemoteHostDetails (final URI uri) {
		this(uri, null);
	}

	public RemoteHostDetails (final URI uri, final String pass) {
		this.uri = uri;
		this.pass = pass;
	}

	public URI getUri () {
		return this.uri;
	}

	public String getPass () {
		return this.pass;
	}

}
