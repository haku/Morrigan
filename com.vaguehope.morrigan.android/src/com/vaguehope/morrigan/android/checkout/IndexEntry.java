package com.vaguehope.morrigan.android.checkout;

import java.util.Collections;
import java.util.List;

public class IndexEntry {

	private final String path;
	private final String hash;
	private final long startCount;
	private final long endCount;
	private final long lastPlayed;
	private final List<String> tags;

	public IndexEntry (
			final String path,
			final String hash,
			final long startCount,
			final long endCount,
			final long lastPlayed,
			final List<String> tags) {
		this.path = path;
		this.hash = hash;
		this.startCount = startCount;
		this.endCount = endCount;
		this.lastPlayed = lastPlayed;
		this.tags = tags;
	}

	public String getPath () {
		return this.path;
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
