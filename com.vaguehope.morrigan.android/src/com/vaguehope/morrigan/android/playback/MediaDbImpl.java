package com.vaguehope.morrigan.android.playback;

import java.math.BigInteger;
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
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import com.vaguehope.morrigan.android.helper.IoHelper;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.helper.StringHelper;

public class MediaDbImpl implements MediaDb {

	protected static final LogWrapper LOG = new LogWrapper("MDI");

	private static final String DB_NAME = "media";
	private static final int DB_VERSION = 6;

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

	// Create and delete libraries.

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
			+ TBL_MF_DBID + "," + TBL_MF_URI + "," + TBL_MF_TITLE + "," + TBL_MF_HASH + ");";

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
			final ContentValues values = new ContentValues();
			for (final QueueItem item : items) {
				if (item.hasRowId()) throw new IllegalArgumentException("QueueItem already has rowId.");
				if (item.hasPosition()) throw new IllegalArgumentException("QueueItem already has position.");

				values.clear();
				copyQueueItemToContentValues(item, values);

				final long newId = this.mDb.insert(TBL_QU, null, values);
				if (newId < 0) throw new IllegalStateException("Adding queue item failed: id=" + newId);

				if (end == QueueEnd.HEAD) {
					moveQueueItemToEndInternal(newId, MoveAction.UP);
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
				final QueueCursorReader reader = new QueueCursorReader();
				final long rowId = reader.readId(c);
				final long position = reader.readPosition(c);
				final long libId = reader.readLibId(c);
				final Uri uri = reader.readUri(c);
				final String title = reader.readTitle(c);
				final long sizeBytes = reader.readSizeBytes(c);
				final long durationMillis = reader.readDurationMillis(c);
				return new QueueItem(rowId,
						position,
						libId,
						uri,
						title,
						sizeBytes,
						durationMillis);
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

	private static void copyMediaItemToContentValues (final MediaItem item, final ContentValues values) {
		values.put(TBL_MF_URI, item.getUri().toString());
		values.put(TBL_MF_TITLE, item.getTitle());
		values.put(TBL_MF_SIZE, item.getSizeBytes());
		values.put(TBL_MF_TIME_LAST_MODIFIED, item.getTimeFileLastModified());
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
	public void rmMediaItemRows (final Collection<Long> rowIds) {
		this.mDb.beginTransaction();
		try {
			for (final Long rowId : rowIds) {
				if (rowId < 0) throw new IllegalArgumentException("MediaItem missing rowId.");

				final int affected = this.mDb.delete(TBL_MF, TBL_MF_ID + "=?", new String[] { String.valueOf(rowId) });
				if (affected > 1) throw new IllegalStateException("Updating media row " + rowId + " affected " + affected + " rows, expected 1.");
				if (affected < 1) LOG.w("Updating media row %s affected %s rows, expected 1.", rowId, affected);
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
						TBL_MF_MISSING,
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
			sql.append(" AND " + TBL_MF_TITLE + " LIKE ? ESCAPE ? COLLATE NOCASE");
			args.add("%" + escapeSearch(query) + "%");
			args.add(SEARCH_ESC);
		}

		return getMfCursor(sql.toString(), args.toArray(new String[args.size()]),
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

	private MediaItem readFirstMediaItemFromCursor (final Cursor c) {
		try {
			if (c != null && c.moveToFirst()) {
				final MediaCursorReader reader = new MediaCursorReader();
				final long rowId = reader.readId(c);
				final long libId = reader.readLibraryId(c);
				final Uri uri = reader.readUri(c);
				final String title = reader.readTitle(c);
				final long sizeBytes = reader.readSizeBytes(c);
				final long timeFileLastModified = reader.readFileLastModified(c);
				final BigInteger fileHash = reader.readFileHash(c);
				final long timeAddedMillis = reader.readTimeAddedMillis(c);
				final long timeLastPlayedMillis = reader.readLastPlayedMillis(c);
				final int startCount = reader.readStartCount(c);
				final int endCount = reader.readEndCount(c);
				final long durationMillis = reader.readDurationMillis(c);
				return new MediaItem(rowId,
						libId,
						uri,
						title,
						sizeBytes,
						timeFileLastModified,
						fileHash,
						timeAddedMillis,
						timeLastPlayedMillis,
						startCount,
						endCount,
						durationMillis);
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
	public MediaItem randomMediaItem (final long libraryId) {
		final Cursor c = getMfCursor(
				TBL_MF_ID + " IN (SELECT " + TBL_MF_ID
				+ " FROM " + TBL_MF
				+ " WHERE " + TBL_MF_DBID + "=?"
				+ " ORDER BY RANDOM() LIMIT 1)",
				new String[] { String.valueOf(libraryId) },
				null, 1);
		return readFirstMediaItemFromCursor(c);
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

			// TODO
			// Tags.
			// Item enabled.

			long earliestTimeAddedMillis = destItem.getTimeAddedMillis();
			long latestTimeLastPlayedMillis = destItem.getTimeLastPlayedMillis();
			int totalStartCount = destItem.getStartCount();
			int totalEndCount = destItem.getEndCount();

			if (totalStartCount < 0) totalStartCount = 0;
			if (totalEndCount < 0) totalEndCount = 0;

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
			}

			final MediaItem newItem = destItem
					.withTimeAdded(earliestTimeAddedMillis)
					.withTimeLastPlayed(latestTimeLastPlayedMillis)
					.withStartCount(totalStartCount)
					.withEndCount(totalEndCount);
			updateMedia(Collections.singleton(newItem));

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
