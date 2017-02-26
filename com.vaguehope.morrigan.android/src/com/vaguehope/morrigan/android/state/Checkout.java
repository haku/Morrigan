package com.vaguehope.morrigan.android.state;

import java.util.Map;

import com.vaguehope.morrigan.android.model.ServerReference;

public class Checkout {

	private final String id;
	private final String hostId;
	private final String dbRelativePath;
	private final String query;
	private final String localDir;
	private final String status;

	public Checkout (final String id, final String hostId, final String dbRelativePath, final String query, final String localDir, final String status) {
		this.id = id;
		this.hostId = hostId;
		this.dbRelativePath = dbRelativePath;
		this.query = query;
		this.localDir = localDir;
		this.status = status;
	}

	public String getId () {
		return this.id;
	}

	public String getHostId () {
		return this.hostId;
	}

	public String getDbRelativePath () {
		return this.dbRelativePath;
	}

	public String getQuery () {
		return this.query;
	}

	public String getLocalDir () {
		return this.localDir;
	}

	public String getStatus () {
		return this.status;
	}

	@Override
	public String toString () {
		return String.format("%s\n%s\n%s\n%s",
				this.dbRelativePath,
				this.query, this.localDir, this.status);
	}

	public String toString (final Map<String, ServerReference> hosts) {
		final ServerReference host = hosts.get(this.hostId);
		return String.format("%s:%s\n%s\n%s\n%s",
				host != null ? host.getName() : this.hostId, this.dbRelativePath,
				this.query, this.localDir, this.status);
	}

	public Checkout withHostId (final String newHostId) {
		return new Checkout(this.id, newHostId, this.dbRelativePath, this.query, this.localDir, this.status);
	}

	public Checkout withDbRelativePath (final String newDbRelativePath) {
		return new Checkout(this.id, this.hostId, newDbRelativePath, this.query, this.localDir, this.status);
	}

	public Checkout withQuery (final String newQuery) {
		return new Checkout(this.id, this.hostId, this.dbRelativePath, newQuery, this.localDir, this.status);
	}

	public Checkout withLocalDir (final String newLocalDir) {
		return new Checkout(this.id, this.hostId, this.dbRelativePath, this.query, newLocalDir, this.status);
	}

	public Checkout withStatus (final String newStatus) {
		return new Checkout(this.id, this.hostId, this.dbRelativePath, this.query, this.localDir,  newStatus);
	}

}
