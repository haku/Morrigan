package com.vaguehope.morrigan.android.playback;

import java.util.Collection;

import android.database.Cursor;
import android.net.Uri;

public interface MediaDb {

	LibraryMetadata newLibrary (String name);
	Collection<LibraryMetadata> getLibraries ();
	LibraryMetadata getLibrary (long libraryId);
	void updateLibrary(LibraryMetadata libraryMetadata);
	void deleteLibrary(LibraryMetadata libraryMetadata);

	void addMedia (long libraryId, Collection<MediaItem> items);
	void updateMedia(long libraryId, Collection<MediaItem> items);
	Cursor getAllMediaCursor (long libraryId, SortColumn sortColumn, SortDirection sortDirection);
	MediaItem getMediaItem(long rowId);
	boolean hasMediaUri(long libraryId, Uri uri);

	void addMediaWatcher(MediaWatcher watcher);
	void removeMediaWatcher(MediaWatcher watcher);

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

	interface MediaWatcher {
		void librariesChanged();
	}

}
