package com.vaguehope.morrigan.android.playback;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio.AudioColumns;
import com.vaguehope.morrigan.android.helper.IoHelper;
import com.vaguehope.morrigan.android.helper.UriHelper;

public class QueueItem {

	private static final String SCHEME_FILE = "file";
	private static final String SCHEME_CONTENT = "content";

	private final long rowId;
	private final long position;
	private final long libraryId;
	private final Uri uri;
	private final String title;
	private final long sizeBytes;
	private final long durationMillis;

	public QueueItem (
			final long rowId,
			final long position,
			final long libraryId,
			final Uri uri,
			final String title,
			final long sizeBytes,
			final long durationMillis) {
		this.rowId = rowId;
		this.position = position;
		this.libraryId = libraryId;
		this.uri = uri;
		this.title = title;
		this.sizeBytes = sizeBytes;
		this.durationMillis = durationMillis;
	}

	public QueueItem (final MediaItem mi) {
		this(-1, -1, mi.getLibraryId(), mi.getUri(), mi.getTitle(), mi.getSizeBytes(), mi.getDurationMillis());
	}

	public QueueItem (final Context context, final QueueItemType type) {
		this(context, -1, type.toUri());
	}

	public QueueItem (final Context context, final Uri uri) {
		this(context, -1, uri);
	}

	public QueueItem (final Context context, final long libraryId, final Uri uri) {
		this.rowId = -1;
		this.position = -1;
		this.libraryId = libraryId;
		this.uri = uri;

		if (uri == null) {
			throw new IllegalArgumentException("URI is required.");
		}

		if (SCHEME_CONTENT.equals(uri.getScheme())) {
			final String[] queryColumns = new String[] {
					AudioColumns.DISPLAY_NAME,
					AudioColumns.SIZE,
					AudioColumns.DURATION,
			};
			final Cursor cursor = context.getContentResolver().query(uri, queryColumns, null, null, null);
			try {
				if (cursor != null) {
					cursor.moveToFirst();
					final int colDisplayName = cursor.getColumnIndex(AudioColumns.DISPLAY_NAME); // Filename with extension.
					final int colSize = cursor.getColumnIndex(AudioColumns.SIZE);
					final int colDuration = cursor.getColumnIndex(AudioColumns.DURATION);
					this.title = cursor.getString(colDisplayName);
					this.sizeBytes = cursor.getLong(colSize);
					this.durationMillis = cursor.getLong(colDuration);
				}
				else {
					throw new IllegalArgumentException("Resource not found: " + uri);
				}
			}
			finally {
				IoHelper.closeQuietly(cursor);
			}
		}
		else if (SCHEME_FILE.equals(uri.getScheme())) {
			this.title = UriHelper.getFileName(uri);
			this.sizeBytes = new File(uri.getPath()).length();
			this.durationMillis = -1;
		}
		else if (QueueItemType.SCHEME.equals(uri.getScheme())) {
			this.title = QueueItemType.parseTitle(uri);
			this.sizeBytes = 0L;
			this.durationMillis = 0L;
		}
		else {
			throw new IllegalArgumentException("Unknown resource type: " + uri);
		}
	}

	public boolean hasRowId () {
		return this.rowId >= 0;
	}

	public long getRowId () {
		return this.rowId;
	}

	public boolean hasPosition () {
		return this.position >= 0;
	}

	public long getPosition () {
		return this.position;
	}

	public boolean hasLibraryId () {
		return this.libraryId >= 0;
	}

	public long getLibraryId () {
		return this.libraryId;
	}

	public Uri getUri () {
		return this.uri;
	}

	public String getTitle () {
		return this.title;
	}

	public long getSizeBytes () {
		return this.sizeBytes;
	}

	public long getDurationMillis () {
		return this.durationMillis;
	}

	@Override
	public String toString () {
		return String.format("%s{%s, \"%s\", %s, %sb, %sms}",
				this.rowId, this.position, this.title, this.uri, this.sizeBytes, this.durationMillis);
	}

}
