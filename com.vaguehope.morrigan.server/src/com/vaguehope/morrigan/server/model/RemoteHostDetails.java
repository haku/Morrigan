package com.vaguehope.morrigan.server.model;

import java.net.URL;

public class RemoteHostDetails {

	private final URL url;
	private final String pass;

	public RemoteHostDetails (final URL url) {
		this(url, null);
	}

	public RemoteHostDetails (final URL url, final String pass) {
		this.url = url;
		this.pass = pass;
	}

	public URL getUrl () {
		return this.url;
	}

	public String getPass () {
		return this.pass;
	}

}
