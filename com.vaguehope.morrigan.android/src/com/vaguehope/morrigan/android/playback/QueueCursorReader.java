package com.vaguehope.morrigan.android.playback;

import android.database.Cursor;
import android.net.Uri;

public class QueueCursorReader {

	private int colId = -1;
	private int colPos = -1;
	private int colLibId = -1;
	private int colUri = -1;
	private int colTitle = -1;
	private int colSize = -1;
	private int colDurationMillis = -1;

	public long readId (final Cursor c) {
		if (c == null) return -1;
		if (this.colId < 0) this.colId = c.getColumnIndexOrThrow(MediaDbImpl.TBL_QU_ID);
		return c.getLong(this.colId);
	}

	public long readPosition (final Cursor c) {
		if (c == null) return -1;
		if (this.colPos < 0) this.colPos = c.getColumnIndexOrThrow(MediaDbImpl.TBL_QU_POSITION);
		return c.getLong(this.colPos);
	}

	public long readLibId (final Cursor c) {
		if (c == null) return -1;
		if (this.colLibId < 0) this.colLibId = c.getColumnIndexOrThrow(MediaDbImpl.TBL_QU_DBID);
		return c.getLong(this.colLibId);
	}

	public Uri readUri (final Cursor c) {
		if (c == null) return null;
		if (this.colUri < 0) this.colUri = c.getColumnIndexOrThrow(MediaDbImpl.TBL_QU_URI);
		return Uri.parse(c.getString(this.colUri));
	}

	public String readTitle (final Cursor c) {
		if (c == null) return null;
		if (this.colTitle < 0) this.colTitle = c.getColumnIndexOrThrow(MediaDbImpl.TBL_QU_TITLE);
		return c.getString(this.colTitle);
	}

	public long readSizeBytes (final Cursor c) {
		if (c == null) return -1;
		if (this.colSize < 0) this.colSize = c.getColumnIndexOrThrow(MediaDbImpl.TBL_QU_SIZE);
		return c.getLong(this.colSize);
	}

	public long readDurationMillis (final Cursor c) {
		if (c == null) return -1;
		if (this.colDurationMillis < 0) this.colDurationMillis = c.getColumnIndexOrThrow(MediaDbImpl.TBL_QU_DURATION_MILLIS);
		return c.getLong(this.colDurationMillis);
	}

	public QueueItem readItem (final Cursor c) {
		final long rowId = readId(c);
		final long position = readPosition(c);
		final long libId = readLibId(c);
		final Uri uri = readUri(c);
		final String title = readTitle(c);
		final long sizeBytes = readSizeBytes(c);
		final long durationMillis = readDurationMillis(c);
		return new QueueItem(rowId,
				position,
				libId,
				uri,
				title,
				sizeBytes,
				durationMillis);
	}

}
