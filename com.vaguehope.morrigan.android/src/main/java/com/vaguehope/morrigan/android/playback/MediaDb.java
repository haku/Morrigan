package com.vaguehope.morrigan.android.playback;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import android.database.Cursor;
import android.net.Uri;

public interface MediaDb {

	void addToQueue(Collection<QueueItem> items, QueueEnd end);
	void removeFromQueue(Collection<QueueItem> items);
	void removeFromQueueById(Collection<Long> itemIds);

	long getQueueSize ();
	Cursor getQueueCursor();
	QueueItem getFirstQueueItem();
	QueueItem getQueueItemById(long rowId);

	void moveQueueItem(long rowId, MoveAction action);
	void moveQueueItemToEnd(long rowId, MoveAction action);

	enum QueueEnd {
		HEAD,
		TAIL;
	}

	enum MoveAction {
		UP,
		DOWN;
	}

	void clearQueue();
	void shuffleQueue();

	LibraryMetadata newLibrary (String name);
	Collection<LibraryMetadata> getLibraries ();
	LibraryMetadata getLibrary (long libraryId);
	void updateLibrary(LibraryMetadata libraryMetadata);
	void deleteLibrary(LibraryMetadata libraryMetadata);

	void addMedia (Collection<MediaItem> items);
	void updateMedia(Collection<MediaItem> items);
	void setFilesExist(Collection<Long> rowId, boolean fileExists);
	void setFileMetadata(long rowId, long fileSize, long fileLastModifiedMillis, BigInteger hash);
	void rmMediaItems (Collection<MediaItem> items);
	void rmMediaItemRows (Collection<Long> rowIds);

	Cursor getAllMediaCursor (long libraryId, SortColumn sortColumn, SortDirection sortDirection);
	Cursor searchMediaCursor (long libraryId, String query, SortColumn sortColumn, SortDirection sortDirection);
	MediaItem getMediaItem(long rowId);
	Presence hasMediaUri(long libraryId, Uri uri);
	/**
	 * URI is unique per library.
	 */
	long getMediaRowId(long libraryId, Uri uri);
	long[] getMediaRowIds(long libraryId, BigInteger hash);

	MediaItem randomMediaItem(long libraryId);

	// Map of rowId to new tags.
	Map<Long, Collection<MediaTag>> readTags (Collection<Long> mfRowIds);
	void updateOriginalHashes (Map<Long, BigInteger> mfRowIdToOriginalHash);
	void updateTimeAdded (Map<Long, Long> mfRowIdToTimeAdded);
	void appendTags (Map<Long, Collection<MediaTag>> mfRowIdToTags);

	Cursor findDuplicates(long libraryId);
	void mergeItems(long destRowId, Collection<Long> fromRowIds);

	void addMediaWatcher(MediaWatcher watcher);
	void removeMediaWatcher(MediaWatcher watcher);

	enum Presence {
		/**
		 * File exists.
		 */
		PRESENT,
		/**
		 * File used to exist, now marked as missing.
		 */
		MISSING,
		/**
		 * File is not known.
		 */
		UNKNOWN;
	}

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
		void queueChanged();
		void librariesChanged();
	}

}
