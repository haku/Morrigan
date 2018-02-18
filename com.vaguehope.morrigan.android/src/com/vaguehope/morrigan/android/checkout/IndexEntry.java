package com.vaguehope.morrigan.android.checkout;

import java.util.Collections;
import java.util.List;

public class IndexEntry {

	private final String localPath;
	private final String hash;
	private final long startCount;
	private final long endCount;
	private final long lastPlayed;
	private final List<String> tags;

	public IndexEntry (
			final String localPath,
			final String hash,
			final long startCount,
			final long endCount,
			final long lastPlayed,
			final List<String> tags) {
		this.localPath = localPath;
		this.hash = hash;
		this.startCount = startCount;
		this.endCount = endCount;
		this.lastPlayed = lastPlayed;
		this.tags = tags;
	}

	/**
	 * Path of checked out file on this device.
	 */
	public String getLocalPath () {
		return this.localPath;
	}

	public String getHash () {
		return this.hash;
	}

	public long getStartCount () {
		return this.startCount;
	}

	public long getEndCount () {
		return this.endCount;
	}

	/**
	 * Milliseconds since epoch.
	 */
	public long getLastPlayed () {
		return this.lastPlayed;
	}

	public List<String> getTags () {
		return Collections.unmodifiableList(this.tags);
	}

}
