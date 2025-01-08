package com.vaguehope.morrigan.model.media.internal.db;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayerChangeListener;
import com.vaguehope.morrigan.model.media.IMixedMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.model.media.MatchMode;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaItemListChangeListener;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.SortColumn;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
import com.vaguehope.morrigan.model.media.internal.CoverArtHelper;
import com.vaguehope.morrigan.model.media.internal.ItemTagsImpl;
import com.vaguehope.morrigan.model.media.internal.MediaItemList;
import com.vaguehope.morrigan.model.media.internal.MediaPictureListHelper;
import com.vaguehope.morrigan.model.media.internal.MediaTagClassificationImpl;
import com.vaguehope.morrigan.model.media.internal.MediaTrackListHelper;
import com.vaguehope.morrigan.player.OrderResolver;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.StringHelper;

public abstract class MediaItemDb extends MediaItemList implements IMediaItemDb {

	private static final SortColumn DEFAULT_SORT_COLUMN = SortColumn.FILE_PATH;

	private final OrderResolver orderResolver = new OrderResolver();
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private final MediaItemDbConfig config;
	private final String searchTerm;

	private IMediaItemStorageLayer dbLayer;
	private SortColumn librarySort;
	private SortDirection librarySortDirection;
	private boolean hideMissing;

	/**
	 * TODO FIXME merge libraryName and searchTerm to match return value of
	 * getSerial().
	 */
	protected MediaItemDb (final String listName, final MediaItemDbConfig config) {
		super(config.getFilePath(), listName);
		this.config = config;

		this.librarySort = DEFAULT_SORT_COLUMN;
		this.librarySortDirection = SortDirection.ASC;
		this.hideMissing = true;

		if (config.getFilter() != null) {
			this.searchTerm = config.getFilter();
		}
		else {
			this.searchTerm = null;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void finalize () throws Throwable {
		dispose();
		super.finalize();
	}

	@Override
	public void dispose () {
		super.dispose();
		this.dbLayer.dispose(); // TODO FIXME what if this layer is shared???  Count attached change listeners?
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof MediaItemDb)) return false;
		final MediaItemDb that = (MediaItemDb) obj;

		return Objects.equals(this.config, that.config);
	}

	@Override
	public int hashCode() {
		return this.config.hashCode();
	}

	public void setDbLayer(final IMediaItemStorageLayer dbLayer) throws DbException {
		if (this.dbLayer != null) throw new IllegalStateException("dbLayer already set: " + this.dbLayer);
		this.dbLayer = dbLayer;

		try {
			readSortFromDb();
		}
		catch (DbException e) {
			e.printStackTrace();
		}
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
	public IMediaItemStorageLayer getDbLayer() {
		if (this.dbLayer == null) throw new IllegalStateException("dbLayer not set.");
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
	public int size() {
		if (!isRead()) return -1;
		return super.size();
	}

	@Override
	public List<IMediaItem> getMediaItems () {
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

		List<IMediaItem> allMedia;
		long t0 = System.currentTimeMillis();
		if (this.searchTerm != null) {
			allMedia = this.dbLayer.getMedia(
					MediaType.TRACK,
					new SortColumn[] { this.librarySort },
					new SortDirection[] { this.librarySortDirection },
					this.hideMissing, this.searchTerm);
		}
		else {
			allMedia = this.dbLayer.getMedia(
					MediaType.TRACK,
					new SortColumn[] { this.librarySort },
					new SortDirection[] { this.librarySortDirection },
					this.hideMissing);
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

	@Override
	public List<IMediaItem> getAllDbEntries () throws DbException {
		// Now that MediaItem classes are shared via factory, this may no longer be needed.
		List<IMediaItem> copyOfMainList = new ArrayList<>(getMediaItems());
		List<IMediaItem> allList = this.dbLayer.getAllMedia(
				new SortColumn[] { DEFAULT_SORT_COLUMN },
				new SortDirection[] { SortDirection.ASC },
				false);
		updateList(copyOfMainList, allList, true);
		return copyOfMainList;
	}

	@Override
	public DurationData getTotalDuration () {
		return MediaTrackListHelper.getTotalDuration(this.getMediaItems());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void setHideMissing(final boolean v) throws MorriganException {
		this.hideMissing = v;
		updateRead();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sorting.

	@Override
	public boolean canSort() {
		return true;
	}

	@Override
	public List<SortColumn> getSuportedSortColumns() {
		final List<SortColumn> ret = new ArrayList<>();
		ret.addAll(Arrays.asList(SortColumn.values()));
		ret.remove(SortColumn.UNSPECIFIED);
		return ret;
	}

	@Override
	public SortColumn getSortColumn() {
		return this.librarySort;
	}

	@Override
	public SortDirection getSortDirection () {
		return this.librarySortDirection;
	}

	@Override
	public void setSort(final SortColumn column, final SortDirection direction) throws MorriganException {
		this.librarySort = column;
		this.librarySortDirection = direction;

		updateRead();

		saveSortToDbInNewThread();
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

		getDbLayer().setProp(KEY_SORTCOL, getSortColumn().name());
		getDbLayer().setProp(KEY_SORTDIR, String.valueOf(getSortDirection().getN()));

		long l1 = System.currentTimeMillis() - t1;
		System.err.println("Saved sort in " + l1 + " ms.");
	}

	private void readSortFromDb () throws DbException {
		String sortcol = getDbLayer().getProp(KEY_SORTCOL);
		String sortdir = getDbLayer().getProp(KEY_SORTDIR);
		if (sortcol != null && sortdir != null) {
			SortColumn col = IMixedMediaItemStorageLayer.parseOldColName(sortcol);
			if (col == null) col = SortColumn.valueOf(sortcol);
			SortDirection dir = SortDirection.parseN(Integer.parseInt(sortdir));
			this.librarySort = col;
			this.librarySortDirection = dir;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public List<PlaybackOrder> getSupportedChooseMethods() {
		final List<PlaybackOrder> ret = new ArrayList<>();
		ret.addAll(Arrays.asList(PlaybackOrder.values()));
		ret.remove(PlaybackOrder.UNSPECIFIED);
		return ret;
	}

	@Override
	public IMediaItem chooseItem(final PlaybackOrder order, final IMediaItem previousItem) {
		return this.orderResolver.getNextTrack(this, previousItem, order);
	}

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
	public IMediaItem getByFile (final File file) throws DbException {
		return this.dbLayer.getByFile(file);
	}

	@Override
	public IMediaItem getByFile (final String filepath) throws DbException {
		return this.dbLayer.getByFile(filepath);
	}

	@Override
	public IMediaItem getByMd5 (final BigInteger md5) throws DbException {
		return this.dbLayer.getByMd5(md5);
	}

	// Search

	@Override
	public List<IMediaItem> search(final MediaType mediaType, final String term, final int maxResults, final SortColumn[] sortColumns, final SortDirection[] sortDirections, final boolean includeDisabled) throws DbException {
		return getDbLayer().search(mediaType, term, maxResults, sortColumns, sortDirections, includeDisabled);
	}

	@Override
	public File findAlbumCoverArt (final MediaAlbum album) throws MorriganException {
		return CoverArtHelper.findCoverArt(getAlbumItems(MediaType.PICTURE, album)); // FIXME set max result count for getAlbumItems().
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB events.

	private final IMediaItemStorageLayerChangeListener storageChangeListener = new IMediaItemStorageLayerChangeListener() {

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
	public void setItemDateAdded (final IMediaItem track, final Date date) throws MorriganException {
		super.setItemDateAdded(track, date);
		try {
			this.dbLayer.setDateAdded(track, date);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void removeItem (final IMediaItem track) throws MorriganException {
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
	private void _removeMediaTrack (final IMediaItem track) throws MorriganException, DbException {
		super.removeItem(track);

		// Remove track.
		if (hasTagsIncludingDeleted(track)) {
			clearTags(track); // Track can not be removed if tags attached (foreign key constraint).
		}

		int n = this.dbLayer.removeFile(track.getFilepath());
		if (n != 1) {
			n = this.dbLayer.removeFile(track);
			if (n != 1) {
				throw new MorriganException("Failed to remove entry from DB by ROWID '" + track.getDbRowId() + "' '" + track.getFilepath() + "'.");
			}
		}
	}

	@Override
	public void setItemMd5 (final IMediaItem track, final BigInteger md5) throws MorriganException {
		super.setItemMd5(track, md5);
		try {
			this.dbLayer.setMd5(track, md5);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemSha1 (final IMediaItem track, final BigInteger sha1) throws MorriganException {
		super.setItemSha1(track, sha1);
		try {
			this.dbLayer.setSha1(track, sha1);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemDateLastModified (final IMediaItem track, final Date date) throws MorriganException {
		super.setItemDateLastModified(track, date);
		try {
			this.dbLayer.setDateLastModified(track, date);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemEnabled (final IMediaItem track, final boolean value) throws MorriganException {
		super.setItemEnabled(track, value);
		try {
			this.dbLayer.setEnabled(track, value);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemEnabled (final IMediaItem track, final boolean value, final Date lastModified) throws MorriganException {
		super.setItemEnabled(track, value, lastModified);
		try {
			this.dbLayer.setEnabled(track, value, lastModified);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemMissing (final IMediaItem track, final boolean value) throws MorriganException {
		super.setItemMissing(track, value);
		try {
			this.dbLayer.setMissing(track, value);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setRemoteLocation (final IMediaItem track, final String remoteLocation) throws MorriganException {
		track.setRemoteLocation(remoteLocation);
		try {
			this.dbLayer.setRemoteLocation(track, remoteLocation);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemMimeType(IMediaItem item, String  newType) throws MorriganException {
		item.setMimeType(newType);
		try {
			this.getDbLayer().setItemMimeType(item, newType);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setItemMediaType (final IMediaItem item, final MediaType newType) throws MorriganException {
		item.setMediaType(newType);
		getChangeEventCaller().mediaItemsUpdated(item);
		this.setDirtyState(DirtyState.METADATA);
		try {
			this.getDbLayer().setItemMediaType(item, newType);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void incTrackStartCnt (final IMediaItem track, final long n) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track, n);
		try {
			this.getDbLayer().incTrackStartCnt(track, n);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void incTrackEndCnt (final IMediaItem track, final long n) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track, n);
		try {
			this.getDbLayer().incTrackEndCnt(track, n);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setTrackDateLastPlayed (final IMediaItem track, final Date date) throws MorriganException {
		MediaTrackListHelper.setDateLastPlayed(this, track, date);
		try {
			this.getDbLayer().setDateLastPlayed(track, date);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void incTrackStartCnt (final IMediaItem track) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track);
		try {
			this.getDbLayer().incTrackPlayed(track);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void incTrackEndCnt (final IMediaItem track) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track);
		try {
			this.getDbLayer().incTrackFinished(track);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setTrackStartCnt (final IMediaItem track, final long n) throws MorriganException {
		MediaTrackListHelper.setTrackStartCnt(this, track, n);
		try {
			this.getDbLayer().setTrackStartCnt(track, n);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setTrackEndCnt (final IMediaItem track, final long n) throws MorriganException {
		MediaTrackListHelper.setTrackEndCnt(this, track, n);
		try {
			this.getDbLayer().setTrackEndCnt(track, n);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setTrackDuration (final IMediaItem track, final int duration) throws MorriganException {
		MediaTrackListHelper.setTrackDuration(this, track, duration);
		try {
			this.getDbLayer().setTrackDuration(track, duration);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setPictureWidthAndHeight (final IMediaItem item, final int width, final int height) throws MorriganException {
		MediaPictureListHelper.setPictureWidthAndHeight(this, item, width, height);
		try {
			this.getDbLayer().setDimensions(item, width, height);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void persistTrackData (final IMediaItem item) throws MorriganException {
		try {
			this.dbLayer.setMd5(item, item.getMd5());
			this.dbLayer.setSha1(item, item.getSha1());
			if (item.getDateAdded() != null) this.dbLayer.setDateAdded(item, item.getDateAdded());
			if (item.getDateLastModified() != null) this.dbLayer.setDateLastModified(item, item.getDateLastModified());
			this.dbLayer.setRemoteLocation(item, item.getRemoteLocation());
			this.dbLayer.setEnabled(item, item.isEnabled(), item.enabledLastModified());
			this.dbLayer.setMissing(item, item.isMissing());
			this.dbLayer.setItemMediaType(item, item.getMediaType());
			this.dbLayer.setTrackStartCnt(item, item.getStartCount());
			this.dbLayer.setTrackEndCnt(item, item.getEndCount());
			this.dbLayer.setTrackDuration(item, item.getDuration());
			if (item.getDateLastPlayed() != null) this.dbLayer.setDateLastPlayed(item, item.getDateLastPlayed());

			this.dbLayer.setDimensions(item, item.getWidth(), item.getHeight());
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
		final Map<String, URI> ret = new LinkedHashMap<>();
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
	public Map<String, MediaTag> tagSearch (final String query, MatchMode mode, final int resLimit) throws MorriganException {
		try {
			return this.dbLayer.tagSearch(query, mode, resLimit);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public boolean hasTagsIncludingDeleted (final IDbItem item) throws MorriganException {
		try {
			return this.dbLayer.hasTagsIncludingDeleted(item);
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
	public Collection<IMediaItem> getAlbumItems(final MediaType mediaType, final MediaAlbum album) throws MorriganException {
		try {
			return this.dbLayer.getAlbumItems(mediaType, album);
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
	public boolean isMarkedAsUnreadable (final IMediaItem mi) throws MorriganException {
		return hasTag(mi, TAG_UNREADABLE, MediaTagType.AUTOMATIC, null);
	}

	@Override
	public void markAsUnreadabled (final IMediaItem mi) throws MorriganException {
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
	public IMediaItem addFile (final MediaType mediaType, final File file) throws MorriganException, DbException {
		boolean added = this.dbLayer.addFile(mediaType, file);
		IMediaItem track = null;
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
	public List<IMediaItem> addFiles (final MediaType mediaType, final List<File> files) throws MorriganException, DbException {
		List<IMediaItem> ret = new ArrayList<>();
		boolean[] res = this.dbLayer.addFiles(mediaType, files);
		for (int i = 0; i < files.size(); i++) {
			if (res[i]) {
				IMediaItem t = getDbLayer().getNewT(files.get(i).getAbsolutePath());
				t.reset();
				addItem(t);
				ret.add(t);
			}
		}
		return ret;
	}

	private List<IMediaItem> _changedItems = null;

	@Override
	public void beginBulkUpdate () {
		if (this._changedItems != null) throw new IllegalArgumentException("beginBulkUpdate() : Build update alredy in progress.");
		this._changedItems = new ArrayList<>();
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
			List<IMediaItem> removed = replaceListWithoutSetDirty(this._changedItems);
			if (!thereWereErrors) {
				this.logger.fine("completeBulkUpdate() : About to clean " + removed.size() + " items...");
				for (final IMediaItem i : removed) {
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
	public IMediaItem updateItem (final IMediaItem mi) throws MorriganException, DbException {
		if (this._changedItems == null) {
			throw new IllegalArgumentException("updateItem() can only be called after beginBulkUpdate() and before completeBulkUpdate().");
		}

		IMediaItem ret;
		boolean added = this.dbLayer.addFile(mi.getMediaType(), mi.getFilepath(), -1);
		if (added) {
			addItem(mi);
			persistTrackData(mi);
			ret = this.dbLayer.getByFile(mi.getFilepath());
		}
		else {
			// Update item.
			IMediaItem track = null;
			List<IMediaItem> mediaTracks = getMediaItems();
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
