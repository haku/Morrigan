package com.vaguehope.morrigan.android.playback;

import java.math.BigInteger;

import android.net.Uri;

public class MediaItem {

	private final long rowId;
	private final Uri uri;
	private final String title;
	private final long sizeBytes;
	private final long timeFileLastModified;
	private final BigInteger fileHash;
	private final long timeAddedMillis;
	private final long timeLastPlayedMillis;
	private final int startCount;
	private final int endCount;
	private final long durationMillis;

	public MediaItem (final Uri uri, final String title,
			final long sizeBytes, final long timeFileLastModified, final long timeAddedMillis) {
		this(-1, uri, title, sizeBytes, timeFileLastModified, null, timeAddedMillis, -1, 0, 0, -1);
	}

	public MediaItem (final long rowId, final Uri uri, final String title,
			final long sizeBytes, final long timeFileLastModified, final BigInteger fileHash,
			final long timeAddedMillis, final long timeLastPlayedMillis,
			final int startCount, final int endCount, final long durationMillis) {
		this.rowId = rowId;
		this.uri = uri;
		this.title = title;
		this.sizeBytes = sizeBytes;
		this.timeFileLastModified = timeFileLastModified;
		this.fileHash = fileHash;
		this.timeAddedMillis = timeAddedMillis;
		this.timeLastPlayedMillis = timeLastPlayedMillis;
		this.startCount = startCount;
		this.endCount = endCount;
		this.durationMillis = durationMillis;
	}

	public long getRowId () {
		return this.rowId;
	}

	public Uri getUri () {
		return this.uri;
	}

	public String getTitle () {
		return this.title;
	}

	public long getsizeBytes () {
		return this.sizeBytes;
	}

	public long getTimeFileLastModified () {
		return this.timeFileLastModified;
	}

	public BigInteger getFileHash () {
		return this.fileHash;
	}

	public long getTimeAddedMillis () {
		return this.timeAddedMillis;
	}

	public long getTimeLastPlayedMillis () {
		return this.timeLastPlayedMillis;
	}

	public int getStartCount () {
		return this.startCount;
	}

	public int getEndCount () {
		return this.endCount;
	}

	public long getDurationMillis () {
		return this.durationMillis;
	}

	@Override
	public String toString () {
		return String.format("%s{\"%s\", %s}",
				this.rowId, this.title, this.uri);
	}

}
