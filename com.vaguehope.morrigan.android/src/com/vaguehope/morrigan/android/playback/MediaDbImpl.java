package com.vaguehope.morrigan.android.playback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.json.JSONException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.vaguehope.morrigan.android.helper.IoHelper;
import com.vaguehope.morrigan.android.helper.LogWrapper;

public class MediaDbImpl implements MediaDb {

	protected static final LogWrapper LOG = new LogWrapper("MDI");

	private static final String DB_NAME = "media";
	private static final int DB_VERSION = 3;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper (final Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate (final SQLiteDatabase db) {
			db.execSQL(TBL_MD_CREATE);
			db.execSQL(TBL_MF_CREATE);
			db.execSQL(TBL_MF_CREATE_INDEX);
		}

		@Override
		public void onUpgrade (final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			LOG.w("Upgrading database from version %d to %d...", oldVersion, newVersion);
			if (oldVersion < 2) { // NOSONAR not a magic number.
				addColumn(db, TBL_MD, TBL_MD_SOURCES, "text");
			}
			if (oldVersion < 3) { // NOSONAR not a magic number.
				addColumn(db, TBL_MF, TBL_MF_TIME_LAST_MODIFIED, "integer");
			}
		}

		@Override
		public void onOpen (final SQLiteDatabase db) {
			super.onOpen(db);
			if (!db.isReadOnly()) {
				db.execSQL("PRAGMA foreign_keys=ON;");
				LOG.i("foreign_keys=ON");
			}
		}

		private static void addColumn (final SQLiteDatabase db, final String table, final String column, final String type) {
			LOG.w("Adding column %s to table %s...", column, table);
			db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type + ";");
		}

	}

	private final Context context;
	private final DatabaseHelper mDbHelper;
	private final SQLiteDatabase mDb;

	private final Set<MediaWatcher> mediaWatchers = new CopyOnWriteArraySet<MediaWatcher>();
	private final MediaWatcherDispatcher mediaWatcherDispatcher = new MediaWatcherDispatcher(this.mediaWatchers);

	public MediaDbImpl (final Context context) {
		this.context = context;
		this.mDbHelper = new DatabaseHelper(this.context);
		this.mDb = this.mDbHelper.getWritableDatabase();
	}

	public void close () {
		this.mDb.close();
		this.mDbHelper.close();
	}

	// Create and delete databases.

	private static final String TBL_MD = "md";
	private static final String TBL_MD_ID = "_id";
	private static final String TBL_MD_NAME = "name";
	private static final String TBL_MD_SOURCES = "sources";

	private static final String TBL_MD_CREATE = "create table " + TBL_MD + " ("
			+ TBL_MD_ID + " integer primary key autoincrement,"
			+ TBL_MD_NAME + " text,"
			+ TBL_MD_SOURCES + " text,"
			+ "UNIQUE(" + TBL_MD_NAME + ") ON CONFLICT ABORT"
			+ ");";

	private static final String TBL_MF = "mf";
	protected static final String TBL_MF_ID = "_id";
	private static final String TBL_MF_DBID = "dbid";
	protected static final String TBL_MF_URI = "uri";
	protected static final String TBL_MF_TITLE = "title";
	protected static final String TBL_MF_SIZE = "size";
	protected static final String TBL_MF_TIME_LAST_MODIFIED = "modified_millis";
	protected static final String TBL_MF_HASH = "hash";
	protected static final String TBL_MF_TIME_ADDED_MILLIS = "added_millis";
	protected static final String TBL_MF_TIME_LAST_PLAYED_MILLIS = "last_played_millis";
	protected static final String TBL_MF_START_COUNT = "start_count";
	protected static final String TBL_MF_END_COUNT = "end_count";
	protected static final String TBL_MF_DURATION_MILLIS = "duration_millis";

	private static final String TBL_MF_CREATE = "create table " + TBL_MF + " ("
			+ TBL_MF_ID + " integer primary key autoincrement,"
			+ TBL_MF_DBID + " integer,"
			+ TBL_MF_URI + " text,"
			+ TBL_MF_TITLE + " text,"
			+ TBL_MF_SIZE + " integer,"
			+ TBL_MF_TIME_LAST_MODIFIED + " integer,"
			+ TBL_MF_HASH + " blob,"
			+ TBL_MF_TIME_ADDED_MILLIS + " integer,"
			+ TBL_MF_TIME_LAST_PLAYED_MILLIS + " integer,"
			+ TBL_MF_START_COUNT + " integer,"
			+ TBL_MF_END_COUNT + " integer,"
			+ TBL_MF_DURATION_MILLIS + " integer,"
			+ "UNIQUE(" + TBL_MF_URI + ") ON CONFLICT ABORT"
			+ ");";

	private static final String TBL_MF_INDEX = TBL_MF + "_idx";
	private static final String TBL_MF_CREATE_INDEX = "CREATE INDEX " + TBL_MF_INDEX + " ON " + TBL_MF + "("
			+ TBL_MF_DBID + "," + TBL_MF_URI + "," + TBL_MF_TITLE + "," + TBL_MF_HASH + ");";

	@Override
	public LibraryMetadata newLibrary (final String name) {
		final ContentValues values = new ContentValues();
		values.put(TBL_MD_NAME, name);

		long newId = -1;
		this.mDb.beginTransaction();
		try {
			newId = this.mDb.insert(TBL_MD, null, values);
			if (newId < 0) throw new IllegalStateException("New DB failed: id=" + newId);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}

		this.mediaWatcherDispatcher.librariesChanged();
		return getLibrary(newId);
	}

	@Override
	public Collection<LibraryMetadata> getLibraries () {
		return queryDbs(null, null, 0);
	}

	@Override
	public LibraryMetadata getLibrary (final long libraryId) {
		if (libraryId < 0) throw new IllegalArgumentException("libraryId must be >=0: libraryId=" + libraryId);
		final Collection<LibraryMetadata> dbs = queryDbs(TBL_MD_ID + "=?", new String[] { String.valueOf(libraryId) }, 2);
		if (dbs.size() < 1) return null;
		if (dbs.size() > 1) throw new IllegalStateException("Multiple DBs with id: " + libraryId);
		return dbs.iterator().next();
	}

	private Collection<LibraryMetadata> queryDbs (final String where, final String[] whereArgs, final int numberOf) {
		final Cursor c = this.mDb.query(true, TBL_MD,
				new String[] { TBL_MD_ID, TBL_MD_NAME, TBL_MD_SOURCES },
				where, whereArgs,
				null, null,
				TBL_MD_ID + " ASC",
				numberOf > 0 ? String.valueOf(numberOf) : null);
		try {
			if (c != null && c.moveToFirst()) {
				final int colId = c.getColumnIndex(TBL_MD_ID);
				final int colName = c.getColumnIndex(TBL_MD_NAME);
				final int colSources = c.getColumnIndex(TBL_MD_SOURCES);

				final List<LibraryMetadata> ret = new ArrayList<LibraryMetadata>();
				do {
					final long id = c.getLong(colId);
					final String name = c.getString(colName);
					final String sources = c.getString(colSources);
					ret.add(new LibraryMetadata(id, name, sources));
				}
				while (c.moveToNext());
				return ret;
			}
			return Collections.EMPTY_LIST;
		}
		catch (final JSONException e) {
			throw new IllegalStateException("Invalid JSON found in DB.", e);
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public void updateLibrary (final LibraryMetadata dbMetadata) {
		final ContentValues values = new ContentValues();
		values.put(TBL_MD_NAME, dbMetadata.getName());
		values.put(TBL_MD_SOURCES, dbMetadata.getSourcesJson().toString());

		boolean success = false;
		this.mDb.beginTransaction();
		try {
			final int affected = this.mDb.update(TBL_MD, values, TBL_MD_ID + "=?", new String[] { String.valueOf(dbMetadata.getId()) });
			if (affected > 1) throw new IllegalStateException("Updating media row " + dbMetadata.getId() + " affected " + affected + " rows, expected 1.");
			if (affected < 1) LOG.w("Updating media row %s affected %s rows, expected 1.", dbMetadata.getId(), affected);
			this.mDb.setTransactionSuccessful();
			success = true;
		}
		finally {
			this.mDb.endTransaction();
		}

		if (success) this.mediaWatcherDispatcher.librariesChanged();
	}

	@Override
	public void deleteLibrary (final LibraryMetadata dbMetadata) {
		boolean success = false;
		this.mDb.beginTransaction();
		try {
			this.mDb.delete(TBL_MD, TBL_MD_ID + "=?",
					new String[] { String.valueOf(dbMetadata.getId()) });
			this.mDb.setTransactionSuccessful();
			success = true;
		}
		finally {
			this.mDb.endTransaction();
		}

		if (success) this.mediaWatcherDispatcher.librariesChanged();
	}

	// Add and query media.

	private static void copyMediaItemToContentValues (final long libraryId, final MediaItem item, final ContentValues values) {
		values.clear();
		values.put(TBL_MF_DBID, libraryId);
		values.put(TBL_MF_URI, item.getUri().toString());
		values.put(TBL_MF_TITLE, item.getTitle());
		values.put(TBL_MF_SIZE, item.getsizeBytes());
		values.put(TBL_MF_TIME_LAST_MODIFIED, item.getTimeFileLastModified());
		if (item.getFileHash() != null) values.put(TBL_MF_HASH, item.getFileHash().toByteArray());
		values.put(TBL_MF_TIME_ADDED_MILLIS, item.getTimeAddedMillis());
		values.put(TBL_MF_TIME_LAST_PLAYED_MILLIS, item.getTimeLastPlayedMillis());
		values.put(TBL_MF_START_COUNT, item.getStartCount());
		values.put(TBL_MF_END_COUNT, item.getEndCount());
		values.put(TBL_MF_DURATION_MILLIS, item.getDurationMillis());
	}

	@Override
	public void addMedia (final long libraryId, final Collection<MediaItem> items) {
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			for (final MediaItem item : items) {
				copyMediaItemToContentValues(libraryId, item, values);
				final long newId = this.mDb.insert(TBL_MF, null, values);
				if (newId < 0) throw new IllegalStateException("Adding media failed: id=" + newId);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	@Override
	public void updateMedia (final long libraryId, final Collection<MediaItem> items) {
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			for (final MediaItem item : items) {
				copyMediaItemToContentValues(libraryId, item, values);
				final int affected = this.mDb.update(TBL_MF, values, TBL_MF_ID + "=?", new String[] { String.valueOf(item.getRowId()) });
				if (affected > 1) throw new IllegalStateException("Updating media row " + item.getRowId() + " affected " + affected + " rows, expected 1.");
				if (affected < 1) LOG.w("Updating media row %s affected %s rows, expected 1.", item.getRowId(), affected);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	private Cursor getMfCursor (final String where, final String[] whereArgs, final String orderBy, final int numberOf) {
		return this.mDb.query(true, TBL_MF,
				new String[] {
						TBL_MF_ID,
						TBL_MF_DBID,
						TBL_MF_URI,
						TBL_MF_TITLE,
						TBL_MF_SIZE,
						TBL_MF_TIME_LAST_MODIFIED,
						TBL_MF_HASH,
						TBL_MF_TIME_ADDED_MILLIS,
						TBL_MF_TIME_LAST_PLAYED_MILLIS,
						TBL_MF_START_COUNT,
						TBL_MF_END_COUNT,
						TBL_MF_DURATION_MILLIS },
				where, whereArgs,
				null, null,
				orderBy,
				numberOf > 0 ? String.valueOf(numberOf) : null);
	}

	private static String toMfSortColumn (final SortColumn sortColumn) {
		switch (sortColumn) {
			case PATH:
				return TBL_MF_URI;
			case DATE_ADDED:
				return TBL_MF_TIME_ADDED_MILLIS;
			case DATE_LAST_PLAYED:
				return TBL_MF_TIME_LAST_PLAYED_MILLIS;
			case START_COUNT:
				return TBL_MF_START_COUNT;
			case END_COUNT:
				return TBL_MF_END_COUNT;
			case DURAITON:
				return TBL_MF_DURATION_MILLIS;
			default:
				throw new IllegalArgumentException("Unknown column: " + sortColumn);
		}
	}

	private static String toSortDirection (final SortDirection sortDirection) {
		switch (sortDirection) {
			case ASC:
				return "ASC";
			case DESC:
				return "DESC";
			default:
				throw new IllegalArgumentException("Unknown direction: " + sortDirection);
		}
	}

	@Override
	public Cursor getAllMediaCursor (final long libraryId, final SortColumn sortColumn, final SortDirection sortDirection) {
		return getMfCursor(
				TBL_MF_DBID + "=?",
				new String[] { String.valueOf(libraryId) },
				toMfSortColumn(sortColumn) + " " + toSortDirection(sortDirection), -1);
	}

	@Override
	public boolean hasMediaUri (final long libraryId, final Uri uri) {
		final Cursor c = getMfCursor(
				TBL_MF_DBID + "=? AND " + TBL_MF_URI + "=?",
				new String[] { String.valueOf(libraryId), uri.toString() },
				null, 1);
		try {
			if (c != null && c.moveToFirst()) {
				return true;
			}
			return false;
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public void addMediaWatcher (final MediaWatcher watcher) {
		this.mediaWatchers.add(watcher);
		watcher.librariesChanged();
	}

	@Override
	public void removeMediaWatcher (final MediaWatcher watcher) {
		this.mediaWatchers.remove(watcher);
	}

}
