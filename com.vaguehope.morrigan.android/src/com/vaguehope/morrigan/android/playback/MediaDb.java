package com.vaguehope.morrigan.android.playback;

import java.util.Collection;

import android.database.Cursor;
import android.net.Uri;

public interface MediaDb {

	DbMetadata newDb (String name);
	Collection<DbMetadata> getDbs ();
	DbMetadata getDb (long dbId);
	void updateDb(DbMetadata dbMetadata);
	void deleteDb(DbMetadata dbMetadata);

	void addMedia (long dbId, Collection<MediaItem> items);
	void updateMedia(long dbId, Collection<MediaItem> items);
	Cursor getAllMediaCursor (long dbId, SortColumn sortColumn, SortDirection sortDirection);
	boolean hasMediaUri(Uri uri);

	void addMediaChangeListener(MediaChangeListener listener);
	void removeMediaChangeListener(MediaChangeListener listener);

	enum SortColumn {
		PATH,
		DATE_ADDED,
		DATE_LAST_PLAYED,
		START_COUNT,
		END_COUNT,
		DURAITON;
	}

	enum SortDirection {
		ASC,
		DESC;
	}

	interface MediaChangeListener {

	}

}
