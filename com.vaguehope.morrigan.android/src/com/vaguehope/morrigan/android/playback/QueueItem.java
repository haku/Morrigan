package com.vaguehope.morrigan.android.playback;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio.AudioColumns;

import com.vaguehope.morrigan.android.helper.IoHelper;
import com.vaguehope.morrigan.android.helper.UriHelper;

public class QueueItem {

	private static final String SCHEME_FILE = "file";
	private static final String SCHEME_CONTENT = "content";

	private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

	private final int queueId;
	private final Uri uri;

	private final CharSequence title;
	private long sizeBytes;
	private long durationMillis;

	public QueueItem (final Context context, final Uri uri) {
		this.queueId = ID_GENERATOR.incrementAndGet();
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
		else {
			throw new IllegalArgumentException("Unknown resource type: " + uri);
		}
	}

	public int getQueueId () {
		return this.queueId;
	}

	public Uri getUri () {
		return this.uri;
	}

	public CharSequence getTitle () {
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
		return String.format("%s{\"%s\", %s, %sb, %sms}",
				this.queueId, this.title, this.uri, this.sizeBytes, this.durationMillis);
	}

}
