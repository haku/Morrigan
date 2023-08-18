package com.vaguehope.morrigan.android.playback;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.vaguehope.morrigan.android.helper.IoHelper;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.helper.StringHelper;

public class MediaDbImpl implements MediaDb {

	protected static final LogWrapper LOG = new LogWrapper("MDI");

	private static final String DB_NAME = "media";
	private static final int DB_VERSION = 8;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper (final Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate (final SQLiteDatabase db) {
			db.execSQL(TBL_QU_CREATE);
			db.execSQL(TBL_QU_POS_TRIGGER);
			db.execSQL(TBL_MD_CREATE);
			db.execSQL(TBL_MF_CREATE);
			db.execSQL(TBL_MF_CREATE_INDEX);
			db.execSQL(TBL_TG_CREATE);
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
			if (oldVersion < 4) { // NOSONAR not a magic number.
				addColumn(db, TBL_MF, TBL_MF_MISSING, "integer");
			}
			if (oldVersion < 5) { // NOSONAR not a magic number.
				db.execSQL(TBL_QU_CREATE);
				db.execSQL(TBL_QU_POS_TRIGGER);
			}
			if (oldVersion < 6) { // NOSONAR not a magic number.
				addColumn(db, TBL_QU, TBL_QU_DBID, "integer");
			}
			if (oldVersion < 7) { // NOSONAR not a magic number.
				db.execSQL(TBL_TG_CREATE);
			}
			if (oldVersion < 8) { // NOSONAR not a magic number.
				addColumn(db, TBL_MF, TBL_MF_OHASH, "blob");
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

	// Queue.

	private static final String TBL_QU = "qu";
	protected static final String TBL_QU_ID = "_id";
	protected static final String TBL_QU_POSITION = "pos";
	protected static final String TBL_QU_DBID = "dbid";
	protected static final String TBL_QU_URI = "uri";
	protected static final String TBL_QU_TITLE = "title";
	protected static final String TBL_QU_SIZE = "size";
	protected static final String TBL_QU_DURATION_MILLIS = "duration_millis";

	private static final String TBL_QU_CREATE = "CREATE TABLE " + TBL_QU + " ("
			+ TBL_QU_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ TBL_QU_POSITION + " INTEGER,"
			+ TBL_QU_DBID + " INTEGER,"
			+ TBL_QU_URI + " TEXT,"
			+ TBL_QU_TITLE + " TEXT,"
			+ TBL_QU_SIZE + " INTEGER,"
			+ TBL_QU_DURATION_MILLIS + " INTEGER,"
			+ "UNIQUE(" + TBL_QU_POSITION + ") ON CONFLICT ABORT"
			+ ");";

	private static final String TBL_QU_POS_TRIGGER = "CREATE TRIGGER qu_set_pos AFTER INSERT ON " + TBL_QU
			+ " BEGIN"
			+ " UPDATE " + TBL_QU + " SET " + TBL_QU_POSITION + "=NEW." + TBL_QU_ID
			+ " WHERE " + TBL_QU_ID + "=NEW." + TBL_QU_ID + ";"
			+ " END;";

	// Libraries.

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
	protected static final String TBL_MF_DBID = "dbid";
	protected static final String TBL_MF_URI = "uri";
	protected static final String TBL_MF_MISSING = "missing";
	protected static final String TBL_MF_TITLE = "title";
	protected static final String TBL_MF_SIZE = "size";
	protected static final String TBL_MF_TIME_LAST_MODIFIED = "modified_millis";
	protected static final String TBL_MF_OHASH = "ohash";
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
			+ TBL_MF_MISSING + " integer,"
			+ TBL_MF_TITLE + " text,"
			+ TBL_MF_SIZE + " integer,"
			+ TBL_MF_TIME_LAST_MODIFIED + " integer,"
			+ TBL_MF_OHASH + " blob,"
			+ TBL_MF_HASH + " blob,"
			+ TBL_MF_TIME_ADDED_MILLIS + " integer,"
			+ TBL_MF_TIME_LAST_PLAYED_MILLIS + " integer,"
			+ TBL_MF_START_COUNT + " integer,"
			+ TBL_MF_END_COUNT + " integer,"
			+ TBL_MF_DURATION_MILLIS + " integer,"
			+ "UNIQUE(" + TBL_MF_DBID + "," + TBL_MF_URI + ") ON CONFLICT ABORT"
			+ ");";

	private static final String TBL_MF_INDEX = TBL_MF + "_idx";
	private static final String TBL_MF_CREATE_INDEX = "CREATE INDEX " + TBL_MF_INDEX + " ON " + TBL_MF + "("
			+ TBL_MF_DBID + "," + TBL_MF_URI + "," + TBL_MF_TITLE + "," + TBL_MF_OHASH + "," + TBL_MF_HASH + ");";

	// Tags.

	private static final String TBL_TG = "tg";
	protected static final String TBL_TG_ID = "_id";
	protected static final String TBL_TG_MFID = "mfid";
	protected static final String TBL_TG_TAG = "tag";
	protected static final String TBL_TG_CLS = "cls";
	protected static final String TBL_TG_TYPE = "typ";
	protected static final String TBL_TG_MODIFIED = "mod";
	protected static final String TBL_TG_DELETED = "del";

	private static final String TBL_TG_CREATE = "create table " + TBL_TG + " ("
			+ TBL_TG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ TBL_TG_MFID + " INTEGER,"
			+ TBL_TG_TAG + " TEXT,"
			+ TBL_TG_CLS + " TEXT,"
			+ TBL_TG_TYPE + " INTEGER,"
			+ TBL_TG_MODIFIED + " INTEGER,"
			+ TBL_TG_DELETED + " INTEGER,"
			+ "FOREIGN KEY(" + TBL_TG_MFID + ")"
					+ " REFERENCES " + TBL_MF + "(" + TBL_MF_ID + ")"
					+ " ON DELETE RESTRICT ON UPDATE RESTRICT,"
			+ "UNIQUE(" + TBL_TG_MFID + "," + TBL_TG_TAG + "," + TBL_TG_CLS + "," + TBL_TG_TYPE + ") ON CONFLICT ABORT"
			+ ");";

//	Queue methods.

	private static void copyQueueItemToContentValues (final QueueItem item, final ContentValues values) {
		// Do not include ID or position.
		values.put(TBL_QU_DBID, item.getLibraryId());
		values.put(TBL_QU_URI, item.getUri().toString());
		values.put(TBL_QU_TITLE, item.getTitle());
		values.put(TBL_QU_SIZE, item.getSizeBytes());
		values.put(TBL_QU_DURATION_MILLIS, item.getDurationMillis());
	}

	@Override
	public void addToQueue (final Collection<QueueItem> items, final QueueEnd end) {
		this.mDb.beginTransaction();
		try {
			final List<Long> addedIds = end == QueueEnd.HEAD ? new ArrayList<Long>() : null;

			final ContentValues values = new ContentValues();
			for (final QueueItem item : items) {
				if (item.hasRowId()) throw new IllegalArgumentException("QueueItem already has rowId.");
				if (item.hasPosition()) throw new IllegalArgumentException("QueueItem already has position.");

				values.clear();
				copyQueueItemToContentValues(item, values);

				final long newId = this.mDb.insert(TBL_QU, null, values);
				if (newId < 0) throw new IllegalStateException("Adding queue item failed: id=" + newId);

				if (addedIds != null) addedIds.add(newId);
			}

			if (addedIds != null) {
				for (int i = addedIds.size() - 1; i >= 0; i--) {
					moveQueueItemToEndInternal(addedIds.get(i), MoveAction.UP);
				}
			}

			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.mediaWatcherDispatcher.queueChanged();
	}

	@Override
	public void removeFromQueue (final Collection<QueueItem> items) {
		final Collection<Long> ids = new ArrayList<Long>(items.size());
		for (final QueueItem item : items) {
			ids.add(item.getRowId());
		}
		removeFromQueueById(ids);
	}

	@Override
	public void removeFromQueueById (final Collection<Long> rowIds) {
		this.mDb.beginTransaction();
		try {
			for (final Long rowId : rowIds) {
				if (rowId == null || rowId < 0) throw new IllegalArgumentException("Invalid rowId: " + rowId);

				final int affected = this.mDb.delete(TBL_QU, TBL_QU_ID + "=?", new String[] { String.valueOf(rowId) });
				if (affected > 1) throw new IllegalStateException("Updating queue row " + rowId + " affected " + affected + " rows, expected 1.");
				if (affected < 1) LOG.w("Updating queue row %s affected %s rows, expected 1.", rowId, affected);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.mediaWatcherDispatcher.queueChanged();
	}

	@Override
	public long getQueueSize () {
		return DatabaseUtils.queryNumEntries(this.mDb, TBL_QU);
	}

	@Override
	public long getQueueDurationMillis () {
		return DatabaseUtils.longForQuery(this.mDb, "SELECT SUM(" + TBL_QU_DURATION_MILLIS + ") FROM " + TBL_QU, null);
	}

	private Cursor getQuCursor (final String where, final String[] whereArgs, final String orderBy, final int numberOf) {
		return this.mDb.query(true, TBL_QU,
				new String[] {
						TBL_QU_ID,
						TBL_QU_POSITION,
						TBL_QU_DBID,
						TBL_QU_URI,
						TBL_QU_TITLE,
						TBL_QU_SIZE,
						TBL_QU_DURATION_MILLIS },
				where, whereArgs,
				null, null,
				orderBy,
				numberOf > 0 ? String.valueOf(numberOf) : null);
	}

	@Override
	public Cursor getQueueCursor () {
		return getQuCursor(null, null, TBL_QU_POSITION + " ASC", -1);
	}

	@Override
	public QueueItem getFirstQueueItem () {
		final Cursor c = getQuCursor(null, null, TBL_QU_POSITION + " ASC", 1);
		return readFirstQueueItemFromCursor(c);
	}

	@Override
	public QueueItem getQueueItemById (final long rowId) {
		final Cursor c = getQuCursor(TBL_QU_ID + "=?", new String[] { String.valueOf(rowId) }, null, 1);
		return readFirstQueueItemFromCursor(c);
	}

	private QueueItem readFirstQueueItemFromCursor (final Cursor c) {
		try {
			if (c != null && c.moveToFirst()) {
				return new QueueCursorReader().readItem(c);
			}
			return null;
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

//	SQLite checks uniqueness constraint after every row, not after the UPDATE. :(

	@Override
	public void moveQueueItemToEnd (final long rowId, final MoveAction action) {
		this.mDb.beginTransaction();
		try {
			moveQueueItemToEndInternal(rowId, action);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.mediaWatcherDispatcher.queueChanged();
	}

	private void moveQueueItemToEndInternal (final long rowId, final MoveAction action) {
		final Long oldPosition = readPositionFromCursor(
				getQuCursor(TBL_QU_ID + "=?", new String[] { String.valueOf(rowId) }, null, 1));
		if (oldPosition == null) throw new IllegalArgumentException("rowId not found: "+ rowId);

		final String sortDirection;
		switch (action) {
			case UP:
				sortDirection = "ASC";
				break;
			case DOWN:
				sortDirection = "DESC";
				break;
			default:
				throw new IllegalArgumentException("Unknown action.");
		}
		final Long newPosition = readPositionFromCursor(
				getQuCursor(null, null,
						TBL_QU_POSITION + " " + sortDirection, 1));
		if (newPosition == null) throw new IllegalArgumentException("End position not found.");

		rollQueueItems(oldPosition, newPosition);
	}

	private void rollQueueItems (final long fromPos, final long toPos) {
		if (fromPos == toPos) return;

		setPositionByPosition(fromPos, -toPos);

		final long offset;
		final long minPosToRoll;
		final long maxPosToRoll;
		if (fromPos > toPos) { // bottom item goes to top.
			offset = 1;
			minPosToRoll = toPos;
			maxPosToRoll = fromPos - 1;
		}
		else { // Top item goes to bottom.
			offset = -1;
			minPosToRoll = fromPos + 1;
			maxPosToRoll = toPos;
		}
		this.mDb.execSQL(
				"UPDATE " + TBL_QU
				+ " SET " + TBL_QU_POSITION + "= - (" + TBL_QU_POSITION + "+?)"
				+ " WHERE " + TBL_QU_POSITION + ">=?"
				+ " AND " + TBL_QU_POSITION + "<=?",
				new String[] {
						String.valueOf(offset),
						String.valueOf(minPosToRoll),
						String.valueOf(maxPosToRoll) });

		// Flip all negatives.
		this.mDb.execSQL(
				"UPDATE " + TBL_QU
				+ " SET " + TBL_QU_POSITION + "= - " + TBL_QU_POSITION
				+ " WHERE " + TBL_QU_POSITION + "<0");
	}

	private void setPositionByRowId (final long rowId, final long newPosition) {
		final ContentValues values = new ContentValues();
		values.put(TBL_QU_POSITION, newPosition);
		final int affected = this.mDb.update(TBL_QU, values, TBL_QU_ID + "=?", new String[] { String.valueOf(rowId) });
		if (affected > 1) throw new IllegalStateException("Updating queue row with ID " + rowId + " affected " + affected + " rows, expected 1.");
		if (affected < 1) LOG.w("Updating queue row with ID %s affected %s rows, expected 1.", rowId, affected);
	}

	private void setPositionByPosition (final long oldPosition, final long newPosition) {
		final ContentValues values = new ContentValues();
		values.put(TBL_QU_POSITION, newPosition);
		final int affected = this.mDb.update(TBL_QU, values, TBL_QU_POSITION + "=?", new String[] { String.valueOf(oldPosition) });
		if (affected > 1) throw new IllegalStateException("Updating queue row with position " + oldPosition + " affected " + affected + " rows, expected 1.");
		if (affected < 1) LOG.w("Updating queue row with position %s affected %s rows, expected 1.", oldPosition, affected);
	}

	private void swapPositions (final long a, final long b) {
		setPositionByPosition(a, -a);
		setPositionByPosition(b, a);
		setPositionByPosition(-a, b);
	}

	@Override
	public void moveQueueItem (final long rowId, final MoveAction action) {
		final String comparison;
		final String sortDirection;
		switch (action) {
			case UP:
				comparison = "<";
				sortDirection = "DESC";
				break;
			case DOWN:
				comparison = ">";
				sortDirection = "ASC";
				break;
			default:
				throw new IllegalArgumentException("Unknown action.");
		}

		this.mDb.beginTransaction();
		try {
			final Long oldPosition = readPositionFromCursor(
					getQuCursor(TBL_QU_ID + "=?", new String[] { String.valueOf(rowId) }, null, 1));
			if (oldPosition == null) throw new IllegalArgumentException("rowId not found: "+ rowId);

			final Long newPosition = readPositionFromCursor(
					getQuCursor(TBL_QU_POSITION + comparison + "?",
							new String[] { String.valueOf(oldPosition) },
							TBL_QU_POSITION + " " + sortDirection, 1));
			if (newPosition == null) return;

			swapPositions(oldPosition, newPosition);

			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.mediaWatcherDispatcher.queueChanged();
	}

	private Long readPositionFromCursor (final Cursor c) {
		try {
			if (c != null && c.moveToFirst()) {
				final QueueCursorReader reader = new QueueCursorReader();
				return reader.readPosition(c);
			}
			return null;
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public void clearQueue () {
		this.mDb.beginTransaction();
		try {
			this.mDb.delete(TBL_QU, null, null);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.mediaWatcherDispatcher.queueChanged();
	}

	@Override
	public void shuffleQueue () {
		this.mDb.beginTransaction();
		try {
			final List<Long> rowIds = new ArrayList<Long>();
			final List<Long> positions = new ArrayList<Long>();

			final Cursor c = getQuCursor(null, null, null, -1);
			try {
				if (c != null && c.moveToFirst()) {
					final QueueCursorReader reader = new QueueCursorReader();
					do {
						final long rowId = reader.readId(c);
						final long position = reader.readPosition(c);
						rowIds.add(rowId);
						positions.add(position);
					}
					while (c.moveToNext());
				}
			}
			finally {
				IoHelper.closeQuietly(c);
			}

			Collections.shuffle(positions);

			for (int i = 0; i < rowIds.size(); i++) {
				final Long rowId = rowIds.get(i);
				final Long position = positions.get(i);
				setPositionByRowId(rowId, -position);
			}

			// Flip all negatives.
			this.mDb.execSQL(
					"UPDATE " + TBL_QU
					+ " SET " + TBL_QU_POSITION + "= - " + TBL_QU_POSITION
					+ " WHERE " + TBL_QU_POSITION + "<0");

			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.mediaWatcherDispatcher.queueChanged();
	}

//	Library methods.

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
			return Collections.emptyList();
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

	private static void copyMediaItemToContentValues (final MediaItem item, final ContentValues values) {
		values.put(TBL_MF_URI, item.getUri().toString());
		values.put(TBL_MF_TITLE, item.getTitle());
		values.put(TBL_MF_SIZE, item.getSizeBytes());
		values.put(TBL_MF_TIME_LAST_MODIFIED, item.getTimeFileLastModified());
		if (item.getFileOriginalHash() != null) values.put(TBL_MF_OHASH, item.getFileOriginalHash().toByteArray());
		if (item.getFileHash() != null) values.put(TBL_MF_HASH, item.getFileHash().toByteArray());
		values.put(TBL_MF_TIME_ADDED_MILLIS, item.getTimeAddedMillis());
		values.put(TBL_MF_TIME_LAST_PLAYED_MILLIS, item.getTimeLastPlayedMillis());
		values.put(TBL_MF_START_COUNT, item.getStartCount());
		values.put(TBL_MF_END_COUNT, item.getEndCount());
		values.put(TBL_MF_DURATION_MILLIS, item.getDurationMillis());
	}

	@Override
	public void addMedia (final Collection<MediaItem> items) {
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			for (final MediaItem item : items) {
				if (item.getRowId() >= 0) throw new IllegalArgumentException("MediaItem already has rowId.");
				if (item.getLibraryId() < 0) throw new IllegalArgumentException("MediaItem missing libraryId.");

				values.clear();
				values.put(TBL_MF_DBID, item.getLibraryId());
				copyMediaItemToContentValues(item, values);

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
	public void updateMedia (final Collection<MediaItem> items) {
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			for (final MediaItem item : items) {
				if (item.getRowId() < 0) throw new IllegalArgumentException("MediaItem missing rowId.");

				values.clear();
				copyMediaItemToContentValues(item, values);
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

	@Override
	public void setFilesExist (final Collection<Long> rowIds, final boolean fileExists) {
		final ContentValues cv = new ContentValues();
		if (fileExists) {
			cv.putNull(TBL_MF_MISSING);
		}
		else {
			cv.put(TBL_MF_MISSING, 1);
		}

		final Collection<RowIdAndValues> values = new ArrayList<MediaDbImpl.RowIdAndValues>(rowIds.size());
		for (final Long rowId : rowIds) {
			values.add(new RowIdAndValues(rowId, cv));
		}

		updateMediaFileRow(values);
	}

	@Override
	public void setFileMetadata (final long rowId, final long fileSize, final long fileLastModifiedMillis, final BigInteger hash) {
		final ContentValues values = new ContentValues();
		values.put(TBL_MF_SIZE, fileSize);
		values.put(TBL_MF_TIME_LAST_MODIFIED, fileLastModifiedMillis);
		values.put(TBL_MF_HASH, hash.toByteArray());
		updateMediaFileRow(Collections.singleton(new RowIdAndValues(rowId, values)));
	}

	private void updateMediaFileRow (final Collection<RowIdAndValues> values) {
		this.mDb.beginTransaction();
		try {
			for (final RowIdAndValues rv : values) {
				final int affected = this.mDb.update(TBL_MF, rv.values, TBL_MF_ID + "=?", new String[] { String.valueOf(rv.rowId) });
				if (affected > 1) throw new IllegalStateException("Updating media row " + rv.rowId + " affected " + affected + " rows, expected 1.");
				if (affected < 1) LOG.w("Updating media row %s affected %s rows, expected 1.", rv.rowId, affected);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	private static class RowIdAndValues {
		final long rowId;
		final ContentValues values;
		public RowIdAndValues (final long rowId, final ContentValues values) {
			this.rowId = rowId;
			this.values = values;
		}
	}

	@Override
	public void rmMediaItems (final Collection<MediaItem> items) {
		this.mDb.beginTransaction();
		try {
			for (final MediaItem item : items) {
				if (item.getRowId() < 0) throw new IllegalArgumentException("MediaItem missing rowId.");

				final int affected = this.mDb.delete(TBL_MF, TBL_MF_ID + "=?", new String[] { String.valueOf(item.getRowId()) });
				if (affected > 1) throw new IllegalStateException("Updating media row " + item.getRowId() + " affected " + affected + " rows, expected 1.");
				if (affected < 1) LOG.w("Updating media row %s affected %s rows, expected 1.", item.getRowId(), affected);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	@Override
	public void rmMediaItemRows (final Collection<Long> mfRowIds) {
		this.mDb.beginTransaction();
		try {
			for (final Long mfRowId : mfRowIds) {
				if (mfRowId < 0) throw new IllegalArgumentException("MediaItem missing rowId.");

				this.mDb.delete(TBL_TG, TBL_TG_MFID + "=?", new String[] { String.valueOf(mfRowId) });
				// Can not meaningfully validate this.

				final int affected = this.mDb.delete(TBL_MF, TBL_MF_ID + "=?", new String[] { String.valueOf(mfRowId) });
				if (affected > 1) throw new IllegalStateException("Deleting media row " + mfRowId + " affected " + affected + " rows, expected 1.");
				if (affected < 1) LOG.w("Updating media row %s affected %s rows, expected 1.", mfRowId, affected);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	private Cursor getMfCursor (final String where, final String[] whereArgs, final String orderBy, final int numberOf) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(TBL_MF);
		return getMfCursor(qb, where, whereArgs, orderBy, numberOf);
	}

	private Cursor getMfCursor (final SQLiteQueryBuilder qb, final String where, final String[] whereArgs, final String orderBy, final int numberOf) {
		qb.setDistinct(true);
		return qb.query(this.mDb,
				new String[] {
						TBL_MF + "." + TBL_MF_ID,
						TBL_MF_DBID,
						TBL_MF_URI,
						TBL_MF_MISSING,
						TBL_MF_TITLE,
						TBL_MF_SIZE,
						TBL_MF_TIME_LAST_MODIFIED,
						TBL_MF_OHASH,
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
				TBL_MF_DBID + "=? AND " + TBL_MF_MISSING + " IS NULL",
				new String[] { String.valueOf(libraryId) },
				toMfSortColumn(sortColumn) + " " + toSortDirection(sortDirection), -1);
	}

	@Override
	public Cursor searchMediaCursor (final long libraryId, final String query, final SortColumn sortColumn, final SortDirection sortDirection) {
		if (StringHelper.isEmpty(query)) return getAllMediaCursor(libraryId, sortColumn, sortDirection);

		final StringBuilder sql = new StringBuilder();
		final List<String> args = new ArrayList<String>();

		sql.append(TBL_MF_DBID + "=?");
		args.add(String.valueOf(libraryId));

		sql.append(" AND " + TBL_MF_MISSING + " IS NULL");

		if (query.startsWith("~")) {
			sql.append(" AND " + TBL_MF_URI + " LIKE ? ESCAPE ? COLLATE NOCASE");
			args.add("%" + escapeSearch(query.substring(1)) + "%");
			args.add(SEARCH_ESC);
		}
		else {
			sql.append(" AND (");

			sql.append(TBL_MF_TITLE + " LIKE ? ESCAPE ? COLLATE NOCASE");
			args.add("%" + escapeSearch(query) + "%");
			args.add(SEARCH_ESC);

			sql.append(" OR ");

			sql.append(TBL_TG_TAG + " LIKE ? ESCAPE ? COLLATE NOCASE");
			args.add("%" + escapeSearch(query) + "%");
			args.add(SEARCH_ESC);

			sql.append(")");
		}

		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(TBL_MF + " INNER JOIN " + TBL_TG + " ON " + TBL_MF + "." + TBL_MF_ID + " = " + TBL_TG_MFID);
		return getMfCursor(qb, sql.toString(), args.toArray(new String[args.size()]),
				toMfSortColumn(sortColumn) + " " + toSortDirection(sortDirection), -1);
	}

	@Override
	public MediaItem getMediaItem (final long rowId) {
		final Cursor c = getMfCursor(
				TBL_MF_ID + "=?",
				new String[] { String.valueOf(rowId) },
				null, 1);
		return readFirstMediaItemFromCursor(c);
	}

	private static MediaItem readFirstMediaItemFromCursor (final Cursor c) {
		try {
			if (c != null && c.moveToFirst()) {
				return new MediaCursorReader().readItem(c);
			}
			return null;
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public Presence hasMediaUri (final long libraryId, final Uri uri) {
		final Cursor c = getMfCursor(
				TBL_MF_DBID + "=? AND " + TBL_MF_URI + "=?",
				new String[] { String.valueOf(libraryId), uri.toString() },
				null, 1);
		try {
			if (c != null && c.moveToFirst()) {
				final int colMissing = c.getColumnIndex(TBL_MF_MISSING);
				return c.isNull(colMissing) ? Presence.PRESENT : Presence.MISSING;
			}
			return Presence.UNKNOWN;
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public long getMediaRowId (final long libraryId, final Uri uri) {
		final Cursor c = getMfCursor(
				TBL_MF_DBID + "=? AND " + TBL_MF_URI + "=?",
				new String[] { String.valueOf(libraryId), uri.toString() },
				null, 1);
		try {
			if (c != null && c.moveToFirst()) {
				final int colId = c.getColumnIndex(TBL_MF_ID);
				return c.getLong(colId);
			}
			return -1;
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public long[] getMediaRowIds (final long libraryId, final BigInteger hash) {
		if (hash == null) throw new IllegalArgumentException("hash is required.");

		final String query = "SELECT " + TBL_MF_ID + " FROM " + TBL_MF + " WHERE "
				+ TBL_MF_DBID + "=? AND (" + TBL_MF_OHASH + "=? OR " + TBL_MF_HASH + "=?)";
		final CursorFactory cursorFactory = new CursorFactory() {
			@Override
			public Cursor newCursor (final SQLiteDatabase db, final SQLiteCursorDriver masterQuery, final String editTable, final SQLiteQuery query) {
				query.bindLong(1, libraryId);
				query.bindBlob(2, hash.toByteArray());
				query.bindBlob(3, hash.toByteArray());
				return new SQLiteCursor(masterQuery, editTable, query);
			}
		};
		final Cursor c = this.mDb.rawQueryWithFactory(cursorFactory, query, null, null);
		try {
			final MediaCursorReader reader = new MediaCursorReader();
			final long[] ret = new long[c.getCount()];
			int i = 0;
			if (c != null && c.moveToFirst()) {
				do {
					ret[i] = reader.readId(c);
					i++;
				}
				while (c.moveToNext());
			}
			if (i < ret.length) throw new IllegalStateException("Expected query to return " + ret.length + " items, but only read " + i + ".");
			return ret;
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public MediaItem randomMediaItem (final long libraryId) {
		final Cursor c = getMfCursor(
				TBL_MF_ID + " IN (SELECT " + TBL_MF_ID
				+ " FROM " + TBL_MF
				+ " WHERE " + TBL_MF_DBID + "=? AND " + TBL_MF_MISSING + " IS NULL"
				+ " ORDER BY RANDOM() LIMIT 1)",
				new String[] { String.valueOf(libraryId) },
				null, 1);
		return readFirstMediaItemFromCursor(c);
	}

	private Cursor getTgCursor (final String where, final String[] whereArgs, final String orderBy, final int numberOf) {
		return this.mDb.query(true, TBL_TG,
				new String[] {
						TBL_TG_ID,
						TBL_TG_MFID,
						TBL_TG_TAG,
						TBL_TG_CLS,
						TBL_TG_TYPE,
						TBL_TG_MODIFIED,
						TBL_TG_DELETED },
				where, whereArgs,
				null, null,
				orderBy,
				numberOf > 0 ? String.valueOf(numberOf) : null);
	}

	private Collection<MediaTag> readTags (final Long mfRowId) {
		final Cursor c = getTgCursor(
				TBL_TG_MFID + "=?",
				new String[]{ String.valueOf(mfRowId) },
				TBL_TG_TAG + " ASC",
				-1);
		try {
			if (c != null && c.moveToFirst()) {
				final Collection<MediaTag> tags = new ArrayList<MediaTag>();
				final TagCursorReader reader = new TagCursorReader();
				do {
					tags.add(reader.readItem(c));
				}
				while (c.moveToNext());
				return tags;
			}
			return Collections.emptyList();
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public Map<Long, Collection<MediaTag>> readTags (final Collection<Long> mfRowIds) {
		final Map<Long, Collection<MediaTag>> ret = new LinkedHashMap<Long, Collection<MediaTag>>();
		for (final Long mfRowId : mfRowIds) {
			ret.put(mfRowId, readTags(mfRowId));
		}
		return ret;
	}

	private MediaTag readTag (final long mfRowId, final String tag, final String cls, final int type) {
		final Cursor c = getTgCursor(
				TBL_TG_MFID + "=? AND " + TBL_TG_TAG + "=? AND " + TBL_TG_CLS + "=? AND " + TBL_TG_TYPE + "=?",
				new String[]{ String.valueOf(mfRowId), tag, cls, String.valueOf(type) },
				TBL_TG_TAG + " ASC",
				-1);
		try {
			if (c != null && c.moveToFirst()) {
				return new TagCursorReader().readItem(c);
			}
			return null;
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public void updateOriginalHashes (final Map<Long, BigInteger> mfRowIdToOriginalHash) {
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();

			for (final Entry<Long, BigInteger> entry : mfRowIdToOriginalHash.entrySet()) {
				final long mfRowId = entry.getKey();
				final BigInteger newOriginalHash = entry.getValue();

				values.clear();
				values.put(TBL_MF_OHASH, newOriginalHash.toByteArray());

				final int affected = this.mDb.update(TBL_MF, values, TBL_MF_ID + "=?", new String[] { String.valueOf(mfRowId) });
				if (affected > 1) throw new IllegalStateException("Updating original hash row with ID " + mfRowId + " affected " + affected + " rows, expected 1.");
				if (affected < 1) LOG.w("Updating original hash row with ID %s affected %s rows, expected 1.", mfRowId, affected);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	@Override
	public void updateDurationSeconds (final Map<Long, Long> mfRowIdToDurationSeconds) {
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();

			for (final Entry<Long, Long> entry : mfRowIdToDurationSeconds.entrySet()) {
				final long mfRowId = entry.getKey();
				final Long durationSeconds = entry.getValue();

				if (durationSeconds <= 0) throw new IllegalArgumentException("New duration must be more than 0: " + durationSeconds);

				values.clear();
				values.put(TBL_MF_DURATION_MILLIS, TimeUnit.SECONDS.toMillis(durationSeconds));

				final int affected = this.mDb.update(TBL_MF, values, TBL_MF_ID + "=?", new String[] { String.valueOf(mfRowId) });
				if (affected > 1) throw new IllegalStateException("Updating duration row with ID " + mfRowId + " affected " + affected + " rows, expected 1.");
				if (affected < 1) LOG.w("Updating duration row with ID %s affected %s rows, expected 1.", mfRowId, affected);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	@Override
	public void updateTimeAdded (final Map<Long, Long> mfRowIdToTimeAdded) {
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();

			for (final Entry<Long, Long> entry : mfRowIdToTimeAdded.entrySet()) {
				final long mfRowId = entry.getKey();
				final Long newTimeAdded = entry.getValue();

				if (newTimeAdded <= 0) throw new IllegalArgumentException("New time added must be more than 0: " + newTimeAdded);

				values.clear();
				values.put(TBL_MF_TIME_ADDED_MILLIS, newTimeAdded);

				final int affected = this.mDb.update(TBL_MF, values,
						TBL_MF_ID + "=? AND " + TBL_MF_TIME_ADDED_MILLIS + ">?",
						new String[] { String.valueOf(mfRowId), String.valueOf(newTimeAdded) });
				if (affected > 1) throw new IllegalStateException("Updating time added row with ID " + mfRowId + " affected " + affected + " rows, expected 1.");
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	@Override
	public void appendTags (final Map<Long, Collection<MediaTag>> mfRowIdToTags) {
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			for (final Entry<Long, Collection<MediaTag>> entry : mfRowIdToTags.entrySet()) {
				final long mfRowId = entry.getKey();
				final Collection<MediaTag> newTags = entry.getValue();
				if (newTags.size() < 1) throw new IllegalArgumentException("Empty tag list for mfRowId: " + mfRowId);
				appendTags(values, mfRowId, newTags);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	private void appendTags (final ContentValues values, final long mfRowId, final Collection<MediaTag> newTags) {
		for (final MediaTag newTag : newTags) {
			values.clear();
			appendTag(values, mfRowId, newTag);
		}
	}

	private void appendTags (final long mfRowId, final Collection<MediaTag> newTags) {
		appendTags(new ContentValues(), mfRowId, newTags);
	}

	private void appendTag (final ContentValues values, final long mfRowId, final MediaTag newTag) {
		final MediaTag existingTag = readTag(mfRowId, newTag.getTag(), newTag.getCls(), newTag.getType().getNumber());
		if (existingTag != null) {
			if (existingTag.hasModified() && newTag.hasModified()
					&& existingTag.getModified() >= newTag.getModified()) {
				return;
			}

			if (existingTag.equalValue(newTag)) {
				return;
			}

			// Update existing tag.
			values.put(TBL_TG_MODIFIED, newTag.getModified());
			putDeleted(newTag.isDeleted(), TBL_TG_DELETED, values);

			final int affected = this.mDb.update(TBL_TG, values, TBL_TG_ID + "=?", new String[] { String.valueOf(existingTag.getRowId()) });
			if (affected > 1) throw new IllegalStateException("Updating tag row with ID " + existingTag.getRowId() + " affected " + affected + " rows, expected 1.");
			if (affected < 1) LOG.w("Updating tag row with ID %s affected %s rows, expected 1.", existingTag.getRowId(), affected);
		}
		else {
			// Insert new tag.
			values.put(TBL_TG_MFID, mfRowId);
			values.put(TBL_TG_TAG, newTag.getTag());
			values.put(TBL_TG_CLS, newTag.getCls());
			values.put(TBL_TG_TYPE, newTag.getType().getNumber());
			values.put(TBL_TG_MODIFIED, newTag.getModified());
			putDeleted(newTag.isDeleted(), TBL_TG_DELETED, values);

			final long newId = this.mDb.insert(TBL_TG, null, values);
			if (newId < 0) throw new IllegalStateException("Adding tag failed: id=" + newId);
		}
	}

	private void putDeleted (final boolean deleted, final String column, final ContentValues values) {
		if (deleted) {
			values.put(column, 1);
		}
		else {
			values.putNull(column);
		}
	}

	// SELECT uri FROM mf WHERE hash IN (
	//   SELECT hash FROM mf WHERE hash IS NOT NULL GROUP BY hash HAVING count(*)>1
	// ) ORDER BY hash ASC;
	@Override
	public Cursor findDuplicates (final long libraryId) {
		final String where = TBL_MF_DBID + "=? AND " + TBL_MF_HASH + " IN ("
				+ "SELECT " + TBL_MF_HASH + " FROM " + TBL_MF
				+ " WHERE " + TBL_MF_DBID + "=? AND " + TBL_MF_HASH + " IS NOT NULL"
				+ " GROUP BY " + TBL_MF_HASH + " HAVING count(*)>1"
				+ ")";
		final String[] whereArgs = new String[] { String.valueOf(libraryId), String.valueOf(libraryId) };
		final String order = TBL_MF_HASH + " ASC";
		return getMfCursor(where, whereArgs, order, 0);
	}

	@Override
	public void mergeItems (final long destRowId, final Collection<Long> fromRowIds) {
		this.mDb.beginTransaction();
		try {
			final MediaItem destItem = getMediaItem(destRowId);

			// TODO Merge item enabled?

			long earliestTimeAddedMillis = destItem.getTimeAddedMillis();
			long latestTimeLastPlayedMillis = destItem.getTimeLastPlayedMillis();
			int totalStartCount = destItem.getStartCount();
			int totalEndCount = destItem.getEndCount();

			if (totalStartCount < 0) totalStartCount = 0;
			if (totalEndCount < 0) totalEndCount = 0;

			final Collection<MediaTag> tagsToAdd = new ArrayList<MediaTag>();

			for (final Long fromRowId : fromRowIds) {
				final MediaItem fromItem = getMediaItem(fromRowId);

				if (fromItem.getTimeAddedMillis() > 0 && earliestTimeAddedMillis > fromItem.getTimeAddedMillis()) {
					earliestTimeAddedMillis = fromItem.getTimeAddedMillis();
				}

				if (fromItem.getTimeLastPlayedMillis() > 0 && latestTimeLastPlayedMillis < fromItem.getTimeLastPlayedMillis()) {
					latestTimeLastPlayedMillis = fromItem.getTimeLastPlayedMillis();
				}

				if (fromItem.getStartCount() > 0) totalStartCount += fromItem.getStartCount();
				if (fromItem.getEndCount() > 0) totalEndCount += fromItem.getEndCount();

				tagsToAdd.addAll(readTags(fromRowId));
			}

			final MediaItem newItem = destItem
					.withTimeAdded(earliestTimeAddedMillis)
					.withTimeLastPlayed(latestTimeLastPlayedMillis)
					.withStartCount(totalStartCount)
					.withEndCount(totalEndCount);
			updateMedia(Collections.singleton(newItem));
			appendTags(destRowId, tagsToAdd);

			rmMediaItemRows(fromRowIds);

			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	@Override
	public void addMediaWatcher (final MediaWatcher watcher) {
		this.mediaWatchers.add(watcher);
		watcher.queueChanged();
		watcher.librariesChanged();
	}

	@Override
	public void removeMediaWatcher (final MediaWatcher watcher) {
		this.mediaWatchers.remove(watcher);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String SEARCH_ESC = "\\";

	/**
	 * This pairs with SEARCH_ESC.
	 */
	private static String escapeSearch (final String term) {
		String q = term.replace("'", "''");
		q = q.replace("\\", "\\\\");
		q = q.replace("%", "\\%");
		q = q.replace("_", "\\_");
		q = q.replace("*", "%");
		return q;
	}

}
