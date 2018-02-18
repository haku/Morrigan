package com.vaguehope.morrigan.android.checkout;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import com.vaguehope.morrigan.android.playback.MediaTag;

public class IndexEntry {

	private final String localPath;
	private final String hash;
	private final long startCount;
	private final long endCount;
	private final long lastPlayed;
	private final List<MediaTag> tags;

	public IndexEntry (
			final String localPath,
			final String hash,
			final long startCount,
			final long endCount,
			final long lastPlayed,
			final List<MediaTag> tags) {
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

	public BigInteger getHash () {
		if (this.hash == null) return null;
		return new BigInteger(this.hash, 16);
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

	public List<MediaTag> getTags () {
		return Collections.unmodifiableList(this.tags);
	}

}
