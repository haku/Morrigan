package com.vaguehope.morrigan.android.playback;

import java.math.BigInteger;

import com.vaguehope.morrigan.android.playback.MediaDb.Presence;

import android.database.Cursor;
import android.net.Uri;

public class MediaCursorReader {

	private int colId = -1;
	private int colLibraryId = -1;
	private int colUri = -1;
	private int colMissing = -1;
	private int colTitle = -1;
	private int colSize = -1;
	private int colLastModified = -1;
	private int colOriginalHash = -1;
	private int colHash = -1;
	private int colAddedMillis = -1;
	private int colLastPlayedMillis = -1;
	private int colStartCount = -1;
	private int colEndCount = -1;
	private int colDurationMillis = -1;

	public long readId (final Cursor c) {
		if (c == null) return -1;
		if (this.colId < 0) this.colId = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_ID);
		return c.getLong(this.colId);
	}

	public long readLibraryId (final Cursor c) {
		if (c == null) return -1;
		if (this.colLibraryId < 0) this.colLibraryId = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_DBID);
		return c.getLong(this.colLibraryId);
	}

	public Uri readUri (final Cursor c) {
		if (c == null) return null;
		if (this.colUri < 0) this.colUri = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_URI);
		return Uri.parse(c.getString(this.colUri));
	}

	public Presence readMissing (final Cursor c) {
		if (c == null) throw new IllegalArgumentException("c can not be null.");
		if (this.colMissing < 0) this.colMissing = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_MISSING);
		return c.isNull(this.colMissing) ? Presence.PRESENT : Presence.MISSING;
	}

	public String readTitle (final Cursor c) {
		if (c == null) return null;
		if (this.colTitle < 0) this.colTitle = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_TITLE);
		return c.getString(this.colTitle);
	}

	public long readSizeBytes (final Cursor c) {
		if (c == null) return -1;
		if (this.colSize < 0) this.colSize = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_SIZE);
		return c.getLong(this.colSize);
	}

	public long readFileLastModified (final Cursor c) {
		if (c == null) return -1;
		if (this.colLastModified < 0) this.colLastModified = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_TIME_LAST_MODIFIED);
		return c.getLong(this.colLastModified);
	}

	public BigInteger readFileOriginalHash (final Cursor c) {
		if (c == null) return null;
		if (this.colOriginalHash < 0) this.colOriginalHash = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_OHASH);
		final byte[] blob = c.getBlob(this.colOriginalHash);
		if (blob == null || blob.length < 1) return null;
		return new BigInteger(blob);
	}

	public BigInteger readFileHash (final Cursor c) {
		if (c == null) return null;
		if (this.colHash < 0) this.colHash = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_HASH);
		final byte[] blob = c.getBlob(this.colHash);
		if (blob == null || blob.length < 1) return null;
		return new BigInteger(blob);
	}

	public long readTimeAddedMillis (final Cursor c) {
		if (c == null) return -1;
		if (this.colAddedMillis < 0) this.colAddedMillis = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_TIME_ADDED_MILLIS);
		return c.getLong(this.colAddedMillis);
	}

	public long readLastPlayedMillis (final Cursor c) {
		if (c == null) return -1;
		if (this.colLastPlayedMillis < 0) this.colLastPlayedMillis = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_TIME_LAST_PLAYED_MILLIS);
		return c.getLong(this.colLastPlayedMillis);
	}

	public int readStartCount (final Cursor c) {
		if (c == null) return -1;
		if (this.colStartCount < 0) this.colStartCount = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_START_COUNT);
		return c.getInt(this.colStartCount);
	}

	public int readEndCount (final Cursor c) {
		if (c == null) return -1;
		if (this.colEndCount < 0) this.colEndCount = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_END_COUNT);
		return c.getInt(this.colEndCount);
	}

	public long readDurationMillis (final Cursor c) {
		if (c == null) return -1;
		if (this.colDurationMillis < 0) this.colDurationMillis = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_DURATION_MILLIS);
		return c.getLong(this.colDurationMillis);
	}

	public MediaItem readItem (final Cursor c) {
		if (c == null) return null;
		final long rowId = readId(c);
		final long libId = readLibraryId(c);
		final Uri uri = readUri(c);
		final String title = readTitle(c);
		final long sizeBytes = readSizeBytes(c);
		final long timeFileLastModified = readFileLastModified(c);
		final BigInteger fileOriginalHash = readFileOriginalHash(c);
		final BigInteger fileHash = readFileHash(c);
		final long timeAddedMillis = readTimeAddedMillis(c);
		final long timeLastPlayedMillis = readLastPlayedMillis(c);
		final int startCount = readStartCount(c);
		final int endCount = readEndCount(c);
		final long durationMillis = readDurationMillis(c);
		return new MediaItem(rowId,
				libId,
				uri,
				title,
				sizeBytes,
				timeFileLastModified,
				fileOriginalHash,
				fileHash,
				timeAddedMillis,
				timeLastPlayedMillis,
				startCount,
				endCount,
				durationMillis);
	}

}
