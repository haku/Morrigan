package com.vaguehope.morrigan.model.media.internal.db;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayerChangeListener;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaItemListChangeListener;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.internal.ItemTagsImpl;
import com.vaguehope.morrigan.model.media.internal.MediaItemList;
import com.vaguehope.morrigan.model.media.internal.MediaTagClassificationImpl;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public abstract class MediaItemDb<S extends IMediaItemStorageLayer<T>, T extends IMediaItem>
		extends MediaItemList<T>
		implements IMediaItemDb<S, T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final boolean HIDEMISSING = true; // TODO link this to GUI?

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private final MediaItemDbConfig config;
	private final String searchTerm;

	private final S dbLayer;
	private IDbColumn librarySort;
	private SortDirection librarySortDirection;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * TODO FIXME merge libraryName and searchTerm to match return value of
	 * getSerial().
	 */
	protected MediaItemDb (final String listName, final MediaItemDbConfig config, final S dbLayer) {
		super(dbLayer.getDbFilePath(), listName);

		this.config = config;
		this.dbLayer = dbLayer;

		this.librarySort = dbLayer.getDefaultSortColumn();
		this.librarySortDirection = SortDirection.ASC;

		if (config.getFilter() != null) {
			this.searchTerm = config.getFilter();
		}
		else {
			this.searchTerm = null;
		}

		try {
			readSortFromDb();
		}
		catch (DbException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void finalize () throws Throwable {
		dispose();
		super.finalize();
	}

	@Override
	public void dispose () {
		super.dispose();
		this._sortChangeListeners.clear();
		this.dbLayer.dispose(); // TODO FIXME what if this layer is shared???  Count attached change listeners?
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Change event listeners.

	/*
	 * This way we only listen for DB events when someone is listening to our
	 * own events. Should help things from getting tangled during GC. Possibly.
	 */

	@Override
	public void addChangeEventListener (final MediaItemListChangeListener listener) {
		if (this.changeEventListeners.size() == 0) this.dbLayer.addChangeListener(this.storageChangeListener);
		super.addChangeEventListener(listener);
	}

	@Override
	public void removeChangeEventListener (final MediaItemListChangeListener listener) {
		super.removeChangeEventListener(listener);
		if (this.changeEventListeners.size() == 0) this.dbLayer.removeChangeListener(this.storageChangeListener);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getSerial () {
		return this.config.getSerial();
	}

	public MediaItemDbConfig getConfig () {
		return this.config;
	}

	@Override
	public S getDbLayer () {
		return this.dbLayer;
	}

	@Override
	public String getSearchTerm () {
		return this.searchTerm;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean isCanBeDirty () {
		return false;
	}

	@Override
	public boolean allowDuplicateEntries () {
		return false;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public int getCount () {
		if (!isRead()) return -1;
		return super.getCount();
	}

	@Override
	public List<T> getMediaItems () {
		if (!isRead()) throw new IllegalStateException("DB has not been loaded.");
		return super.getMediaItems();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String KEY_UUID = "UUID";

	@Override
	public synchronized UUID getUuid () {
		try {
			{
				final String uuid = this.dbLayer.getProp(KEY_UUID);
				if (StringHelper.notBlank(uuid)) return UUID.fromString(uuid);
			}
			{
				this.dbLayer.setProp(KEY_UUID, UUID.randomUUID().toString());
				final String uuid = this.dbLayer.getProp(KEY_UUID);
				if (StringHelper.notBlank(uuid)) return UUID.fromString(uuid);
			}
			throw new IllegalStateException("UUID I just wrote to the DB is not there. :(");
		}
		catch (DbException e) {
			throw new IllegalStateException(e.toString(), e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private boolean firstRead = true;
	private long durationOfLastRead = -1;

	/**
	 * @return true if DB state has been loaded.
	 */
	protected boolean isRead () {
		return !this.firstRead;
	}

	@Override
	public void read () throws MorriganException {
		if (isRead()) return;
		try {
			doRead();
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	/**
	 * @throws MorriganException
	 */
	protected void doRead () throws MorriganException, DbException {
//		System.err.println("[?] reading... " + getType() + " " + getListName() + "...");

		List<T> allMedia;
		long t0 = System.currentTimeMillis();
		if (this.searchTerm != null) {
			allMedia = this.dbLayer.getMedia(
					new IDbColumn[] { this.librarySort },
					new SortDirection[] { this.librarySortDirection },
					HIDEMISSING, this.searchTerm);
		}
		else {
			allMedia = this.dbLayer.getMedia(
					new IDbColumn[] { this.librarySort },
					new SortDirection[] { this.librarySortDirection },
					HIDEMISSING);
		}
		long l0 = System.currentTimeMillis() - t0;

		long t1 = System.currentTimeMillis();
		setMediaTracks(allMedia);
		long l1 = System.currentTimeMillis() - t1;

//		System.err.println("[" + l0 + "," + l1 + " ms] " + getType() + " " + getListName());
		this.durationOfLastRead = l0 + l1;

		this.firstRead = false;

		this.getChangeEventCaller().mediaListRead();
	}

	@Override
	public long getDurationOfLastRead () {
		return this.durationOfLastRead;
	}

	/**
	 * FIXME Sort library list according to DB query. Clear then reload? How
	 * about loading into a new list, then replacing? Would need to be
	 * thread-safe.
	 * @throws MorriganException
	 */
	@Override
	public void forceRead () throws MorriganException {
		// TODO FIXME Lol these next 2 lines are not thread safe.
		this.firstRead = true;
		read();
	}

	/**
	 * Only read if already read. No point re-reading if no one is expecting it
	 * to already be read.
	 */
	public void updateRead () throws MorriganException {
		if (isRead()) {
			forceRead();
		}
		else {
			this.logger.fine("Skipping reRead() because its un-needed.");
		}
	}

	@Override
	public void commitOrRollback () throws DbException {
		this.dbLayer.commitOrRollBack();
	}

	@Override
	public void rollback () throws DbException {
		this.dbLayer.rollback();
	}

	/**
	 * Returns a copy of the main list updated with all items from the DB.
	 */
	@Override
	public List<T> getAllDbEntries () throws DbException {
		// Now that MediaItem classes are shared via factory, this may no longer be needed.
		List<T> copyOfMainList = new ArrayList<T>(getMediaItems());
		List<T> allList = this.dbLayer.getAllMedia(
				new IDbColumn[] { this.dbLayer.getDefaultSortColumn() },
				new SortDirection[] { SortDirection.ASC },
				false);
		updateList(copyOfMainList, allList, true);
		return copyOfMainList;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sorting.

	@Override
	public IDbColumn getSort () {
		return this.librarySort;
	}

	@Override
	public SortDirection getSortDirection () {
		return this.librarySortDirection;
	}

	@Override
	public void setSort (final IDbColumn sort, final SortDirection direction) throws MorriganException {
		this.librarySort = sort;
		this.librarySortDirection = direction;

		updateRead();
		callSortChangedListeners(this.librarySort, this.librarySortDirection);

		saveSortToDbInNewThread();
	}

	private final List<SortChangeListener> _sortChangeListeners = Collections.synchronizedList(new ArrayList<SortChangeListener>());

	private void callSortChangedListeners (final IDbColumn sort, final SortDirection direction) {
		for (SortChangeListener l : this._sortChangeListeners) {
			l.sortChanged(sort, direction);
		}
	}

	@Override
	public void registerSortChangeListener (final SortChangeListener scl) {
		this._sortChangeListeners.add(scl);
	}

	@Override
	public void unregisterSortChangeListener (final SortChangeListener scl) {
		this._sortChangeListeners.remove(scl);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sort saving.

	public static final String KEY_SORTCOL = "SORTCOL";
	public static final String KEY_SORTDIR = "SORTDIR";

	private void saveSortToDbInNewThread () {
		new Thread() {
			@Override
			public void run () {
				try {
					saveSortToDb();
				}
				catch (DbException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	void saveSortToDb () throws DbException {
		long t1 = System.currentTimeMillis();

		getDbLayer().setProp(KEY_SORTCOL, getSort().getName());
		getDbLayer().setProp(KEY_SORTDIR, String.valueOf(getSortDirection().getN()));

		long l1 = System.currentTimeMillis() - t1;
		System.err.println("Saved sort in " + l1 + " ms.");
	}

	private void readSortFromDb () throws DbException {
		String sortcol = getDbLayer().getProp(KEY_SORTCOL);
		String sortdir = getDbLayer().getProp(KEY_SORTDIR);
		if (sortcol != null && sortdir != null) {
			IDbColumn col = parseColumnFromName(sortcol);
			SortDirection dir = SortDirection.parseN(Integer.parseInt(sortdir));
			this.librarySort = col;
			this.librarySortDirection = dir;
		}
	}

	protected abstract IDbColumn parseColumnFromName (String name);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queries.

	@Override
	public FileExistance hasFile (final String filepath) throws MorriganException, DbException {
		return this.dbLayer.hasFile(filepath);
	}

	@Override
	public FileExistance hasFile (final File file) throws MorriganException, DbException {
		return this.dbLayer.hasFile(file);
	}

	@Override
	public T getByFile (final File file) throws DbException {
		return this.dbLayer.getByFile(file);
	}

	@Override
	public T getByFile (final String filepath) throws DbException {
		return this.dbLayer.getByFile(filepath);
	}

	@Override
	public T getByHashcode (final BigInteger hashcode) throws DbException {
		return this.dbLayer.getByHashcode(hashcode);
	}

	@Override
	public List<T> simpleSearch (final String term, final int maxResults) throws DbException {
		return this.dbLayer.simpleSearch(term, maxResults);
	}

	@Override
	public List<T> simpleSearch (final String term, final int maxResults, final IDbColumn[] sortColumns, final SortDirection[] sortDirections, final boolean includeDisabled) throws DbException {
		return this.dbLayer.simpleSearch(term, maxResults, sortColumns, sortDirections, includeDisabled);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB events.

	private final IMediaItemStorageLayerChangeListener<T> storageChangeListener = new IMediaItemStorageLayerChangeListener<T>() {

		@Override
		public void eventMessage (final String msg) {
			getChangeEventCaller().eventMessage(msg);
		}

		@Override
		public void propertySet (final String key, final String value) {
			// Unused.
		}

		@Override
		public void mediaItemAdded (final String filePath) {
			getChangeEventCaller().mediaItemsAdded((IMediaItem[]) null); // TODO pass-through actual item?
		}

		@Override
		public void mediaItemsAdded (final List<File> filePaths) {
			getChangeEventCaller().mediaItemsAdded((IMediaItem[]) null); // TODO pass-through actual item?
		}

		@Override
		public void mediaItemRemoved (final String filePath) {
			getChangeEventCaller().mediaItemsRemoved((IMediaItem[]) null); // TODO pass-through actual item?
		}

		@Override
		public void mediaItemUpdated (final IMediaItem item) {
			getChangeEventCaller().mediaItemsUpdated(item);
		}

		@Override
		public void mediaItemTagAdded (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc) {
			if (MediaItemDb.this.getConfig().getFilter() != null) { // TODO make more specific?
				getChangeEventCaller().mediaItemsForceReadRequired((IMediaItem[]) null); // TODO pass-through actual item?
			}
		}

		@Override
		public void mediaItemTagsMoved (final IDbItem from_item, final IDbItem to_item) {
			if (MediaItemDb.this.getConfig().getFilter() != null) { // TODO make more specific?
				getChangeEventCaller().mediaItemsForceReadRequired((IMediaItem[]) null); // TODO pass-through actual item?
			}
		}

		@Override
		public void mediaItemTagRemoved (final MediaTag tag) {
			if (MediaItemDb.this.getConfig().getFilter() != null) { // TODO make more specific?
				getChangeEventCaller().mediaItemsForceReadRequired((IMediaItem[]) null); // TODO pass-through actual item?
			}
		}

		@Override
		public void mediaItemTagsCleared (final IDbItem item) {
			if (MediaItemDb.this.getConfig().getFilter() != null) { // TODO make more specific?
				getChangeEventCaller().mediaItemsForceReadRequired((IMediaItem[]) null); // TODO pass-through actual item?
			}
		}

	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Updating tracks.

	@Override
	public void setItemDateAdded (final T track, final Date date) throws MorriganException {
		super.setItemDateAdded(track, date);
		try {
			this.dbLayer.setDateAdded(track, date);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void removeItem (final T track) throws MorriganException {
		try {
			_removeMediaTrack(track);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	/**
	 * This is so that this class can always call this method, even when this
	 * class is sub-classed and removeMediaTrack() overridden.
	 * @throws DbException
	 */
	private void _removeMediaTrack (final T track) throws MorriganException, DbException {
		super.removeItem(track);

		// Remove track.
		if (this.hasTags(track)) this.dbLayer.clearTags(track); // Track can not be removed if tags attached (foreign key constraint).
		int n = this.dbLayer.removeFile(track.getFilepath());
		if (n != 1) {
			n = this.dbLayer.removeFile(track);
			if (n != 1) {
				throw new MorriganException("Failed to remove entry from DB by ROWID '" + track.getDbRowId() + "' '" + track.getFilepath() + "'.");
			}
		}

		// Remove tags.
		if (hasTags(track)) {
			clearTags(track);
		}
	}

	@Override
	public void setItemHashCode (final T track, final BigInteger hashcode) throws MorriganException {
		super.setItemHashCode(track, hashcode);
		try {
			this.dbLayer.setHashcode(track, hashcode);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemDateLastModified (final T track, final Date date) throws MorriganException {
		super.setItemDateLastModified(track, date);
		try {
			this.dbLayer.setDateLastModified(track, date);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemEnabled (final T track, final boolean value) throws MorriganException {
		super.setItemEnabled(track, value);
		try {
			this.dbLayer.setEnabled(track, value);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemEnabled (final T track, final boolean value, final Date lastModified) throws MorriganException {
		super.setItemEnabled(track, value, lastModified);
		try {
			this.dbLayer.setEnabled(track, value, lastModified);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemMissing (final T track, final boolean value) throws MorriganException {
		super.setItemMissing(track, value);
		try {
			this.dbLayer.setMissing(track, value);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setRemoteLocation (final T track, final String remoteLocation) throws MorriganException {
		track.setRemoteLocation(remoteLocation);
		try {
			this.dbLayer.setRemoteLocation(track, remoteLocation);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void persistTrackData (final T track) throws MorriganException {
		try {
			this.dbLayer.setHashcode(track, track.getHashcode());
			if (track.getDateAdded() != null) this.dbLayer.setDateAdded(track, track.getDateAdded());
			if (track.getDateLastModified() != null) this.dbLayer.setDateLastModified(track, track.getDateLastModified());
			this.dbLayer.setRemoteLocation(track, track.getRemoteLocation());
			this.dbLayer.setEnabled(track, track.isEnabled(), track.enabledLastModified());
			this.dbLayer.setMissing(track, track.isMissing());
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getDbPath () {
		return this.dbLayer.getDbFilePath();
	}

	@Override
	public List<String> getSources () throws MorriganException {
		try {
			return this.dbLayer.getSources();
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void addSource (final String source) throws MorriganException {
		try {
			this.dbLayer.addSource(source);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void removeSource (final String source) throws MorriganException {
		try {
			this.dbLayer.removeSource(source);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	private static final String REMOTE_PROP_KEY_PREFIX = "REMOTE_";

	@Override
	public void addRemote (final String name, final URI uri) throws DbException {
		if (StringHelper.blank(name)) throw new IllegalArgumentException("Name must not be blank.");
		if (uri == null) throw new IllegalArgumentException("URI must not be null.");
		this.dbLayer.setProp(REMOTE_PROP_KEY_PREFIX + name, uri.toString());
	}

	@Override
	public void rmRemote (final String name) throws DbException {
		this.dbLayer.setProp(REMOTE_PROP_KEY_PREFIX + name, null);
	}

	@Override
	public URI getRemote (final String name) throws DbException {
		final String prop = this.dbLayer.getProp(REMOTE_PROP_KEY_PREFIX + name);
		if (StringHelper.blank(prop)) return null;
		try {
			return new URI(prop);
		}
		catch (final URISyntaxException e) {
			throw new DbException("Remote " + name + " is invalid URI: " + prop, e);
		}
	}

	@Override
	public Map<String, URI> getRemotes () throws DbException {
		final Map<String, URI> ret = new LinkedHashMap<String, URI>();
		for (final Entry<String, String> prop : this.dbLayer.getProps().entrySet()) {
			if (prop.getKey().startsWith(REMOTE_PROP_KEY_PREFIX)) try {
				ret.put(StringHelper.removeStart(prop.getKey(), REMOTE_PROP_KEY_PREFIX), new URI(prop.getValue()));
			}
			catch (final URISyntaxException e) {
				throw new DbException("Remote " + prop.getKey() + " is invalid URI: " + prop.getValue(), e);
			}
		}
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Tags.

	@Override
	public List<MediaTag> getTopTags (final int countLimit) throws MorriganException {
		try {
			return this.dbLayer.getTopTags(countLimit);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public Map<String, MediaTag> tagSearch (final String prefix, final int resLimit) throws MorriganException {
		try {
			return this.dbLayer.tagSearch(prefix, resLimit);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public boolean hasTags (final IDbItem item) throws MorriganException {
		try {
			return this.dbLayer.hasTags(item);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public boolean hasTag (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc) throws MorriganException {
		try {
			return this.dbLayer.hasTag(item, tag, type, mtc);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public List<MediaTag> getTags (final IDbItem item) throws MorriganException {
		try {
			return this.dbLayer.getTags(item, false);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public List<MediaTag> getTagsIncludingDeleted (final IDbItem item) throws MorriganException {
		try {
			return this.dbLayer.getTags(item, true);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public ItemTags readTags (final IDbItem item) throws MorriganException {
		return ItemTagsImpl.forItem(this, item);
	}

	@Override
	public void addTag (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc) throws MorriganException {
		try {
			this.dbLayer.addTag(item, tag, type, mtc);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void addTag (final IDbItem item, final String tag, final MediaTagType type, final String mtc) throws MorriganException {
		try {
			this.dbLayer.addTag(item, tag, type, mtc);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void addTag (final IDbItem item, final String tag, final MediaTagType type, final String mtc, final Date modified, final boolean deleted) throws MorriganException {
		try {
			this.dbLayer.addTag(item, tag, type, mtc, modified, deleted);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void moveTags (final IDbItem from_item, final IDbItem to_item) throws MorriganException {
		try {
			this.dbLayer.moveTags(from_item, to_item);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void removeTag (final MediaTag mt) throws MorriganException {
		try {
			this.dbLayer.removeTag(mt);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void clearTags (final IDbItem item) throws MorriganException {
		try {
			this.dbLayer.clearTags(item);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public List<MediaTagClassification> getTagClassifications () throws MorriganException {
		try {
			return this.dbLayer.getTagClassifications();
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public MediaTagClassification getTagClassification (final String classificationName) throws MorriganException {
		try {
			return this.dbLayer.getTagClassification(classificationName);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void addTagClassification (final String classificationName) throws MorriganException {
		try {
			this.dbLayer.addTagClassification(classificationName);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Albums.

	@Override
	public MediaAlbum createAlbum (final String name) throws MorriganException {
		try {
			return this.dbLayer.createAlbum(name);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public MediaAlbum getAlbum (final String name) throws MorriganException {
		try {
			return this.dbLayer.getAlbum(name);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void removeAlbum (final MediaAlbum album) throws MorriganException {
		try {
			this.dbLayer.removeAlbum(album);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public Collection<MediaAlbum> getAlbums () throws MorriganException {
		try {
			return this.dbLayer.getAlbums();
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public Collection<T> getAlbumItems (final MediaAlbum album) throws MorriganException {
		try {
			return this.dbLayer.getAlbumItems(album);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void addToAlbum (final MediaAlbum album, final IDbItem item) throws MorriganException {
		try {
			this.dbLayer.addToAlbum(album, item);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void removeFromAlbum (final MediaAlbum album, final IDbItem item) throws MorriganException {
		try {
			this.dbLayer.removeFromAlbum(album, item);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public int removeFromAllAlbums (final IDbItem item) throws MorriganException {
		try {
			return this.dbLayer.removeFromAllAlbums(item);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Meta tagging.

	private static final String TAG_UNREADABLE = "UNREADABLE";

	@Override
	public boolean isMarkedAsUnreadable (final T mi) throws MorriganException {
		return hasTag(mi, TAG_UNREADABLE, MediaTagType.AUTOMATIC, null);
	}

	@Override
	public void markAsUnreadabled (final T mi) throws MorriganException {
		setItemEnabled(mi, false);
		addTag(mi, TAG_UNREADABLE, MediaTagType.AUTOMATIC, (MediaTagClassificationImpl) null);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Returns object if the file was added. (i.e. it was not already in the
	 * library)
	 * @throws MorriganException
	 * @throws DbException
	 */
	@Override
	public T addFile (final File file) throws MorriganException, DbException {
		T track = null;
		boolean added = this.dbLayer.addFile(file);
		if (added) {
			track = getDbLayer().getNewT(file.getAbsolutePath());
			track.reset();
			addItem(track);
		}
		return track;
	}

	/**
	 * Returns list of objects that were added. Files that were already present
	 * are ignored.
	 */
	@Override
	public List<T> addFiles (final List<File> files) throws MorriganException, DbException {
		List<T> ret = new ArrayList<T>();
		boolean[] res = this.dbLayer.addFiles(files);
		for (int i = 0; i < files.size(); i++) {
			if (res[i]) {
				T t = getDbLayer().getNewT(files.get(i).getAbsolutePath());
				t.reset();
				addItem(t);
				ret.add(t);
			}
		}
		return ret;
	}

	private List<T> _changedItems = null;

	@Override
	public void beginBulkUpdate () {
		if (this._changedItems != null) throw new IllegalArgumentException("beginBulkUpdate() : Build update alredy in progress.");
		this._changedItems = new ArrayList<T>();
	}

	/**
	 * This method does NOT update the in-memory model or the UI, but instead
	 * assume you are about to re-query the DB. You will want to do this to get
	 * fresh data that is sorted in the correct order.
	 * @param thereWereErrors
	 * @throws MorriganException
	 * @throws DbException
	 */
	@Override
	public void completeBulkUpdate (final boolean thereWereErrors) throws MorriganException, DbException {
		try {
			List<T> removed = replaceListWithoutSetDirty(this._changedItems);
			if (!thereWereErrors) {
				this.logger.fine("completeBulkUpdate() : About to clean " + removed.size() + " items...");
				for (T i : removed) {
					_removeMediaTrack(i);
				}
			}
			else {
				this.logger.fine("completeBulkUpdate() : Errors occured, skipping delete.");
			}
		}
		finally {
			this._changedItems = null;
		}
	}

	@Override
	public T updateItem (final T mi) throws MorriganException, DbException {
		if (this._changedItems == null) {
			throw new IllegalArgumentException("updateItem() can only be called after beginBulkUpdate() and before completeBulkUpdate().");
		}

		T ret;
		boolean added = this.dbLayer.addFile(mi.getFilepath(), -1);
		if (added) {
			addItem(mi);
			persistTrackData(mi);
			ret = this.dbLayer.getByFile(mi.getFilepath());
		}
		else {
			// Update item.
			T track = null;
			List<T> mediaTracks = getMediaItems();
			int index = mediaTracks.indexOf(mi); // TODO FIXME This REALLY should be a HashMap lookup.
			if (index >= 0) {
				track = mediaTracks.get(index);
			}
			else {
				throw new MorriganException("updateItem() : Failed to find item '" + mi.getFilepath() + "' in list '" + this + "'.");
			}
			if (track.setFromMediaItem(mi)) {
				getChangeEventCaller().mediaItemsUpdated(track);
				setDirtyState(DirtyState.DIRTY); // just to trigger change events.
				persistTrackData(track);
			}
			ret = track;
		}

		if (this._changedItems != null) this._changedItems.add(mi);

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
