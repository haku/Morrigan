package com.vaguehope.morrigan.android.state;

public class Checkout {

	private final String id;
	private final String hostId;
	private final String query;
	private final String localDir;

	public Checkout (final String hostId, final String query, final String localDir) {
		this(null, hostId, query, localDir);
	}

	public Checkout (final String id, final String hostId, final String query, final String localDir) {
		this.id = id;
		this.hostId = hostId;
		this.query = query;
		this.localDir = localDir;
	}

	public String getId () {
		return this.id;
	}

	public String getHostId () {
		return this.hostId;
	}

	public String getQuery () {
		return this.query;
	}

	public String getLocalDir () {
		return this.localDir;
	}

	@Override
	public String toString () {
		return String.format("%s\n%s", this.query, this.localDir);
	}

	public Checkout withHostId (final String newHostId) {
		return new Checkout(this.id, newHostId, this.query, this.localDir);
	}

}
