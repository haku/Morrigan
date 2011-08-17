package net.sparktank.morrigan.model.media.internal.db;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.vaguehope.sqlitewrapper.DbException;

import net.sparktank.morrigan.model.db.IDbColumn;
import net.sparktank.morrigan.model.db.IDbItem;
import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.DirtyState;
import net.sparktank.morrigan.model.media.IMediaItem;
import net.sparktank.morrigan.model.media.IMediaItemDb;
import net.sparktank.morrigan.model.media.IMediaItemStorageLayer;
import net.sparktank.morrigan.model.media.IMediaItemStorageLayerChangeListener;
import net.sparktank.morrigan.model.media.MediaItemListChangeListener;
import net.sparktank.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import net.sparktank.morrigan.model.media.internal.MediaItemList;
import net.sparktank.morrigan.model.media.internal.MediaTagClassificationImpl;
import net.sparktank.morrigan.model.media.MediaTag;
import net.sparktank.morrigan.model.media.MediaTagClassification;
import net.sparktank.morrigan.model.media.MediaTagType;

public abstract class MediaItemDb<H extends IMediaItemDb<H,S,T>, S extends IMediaItemStorageLayer<T>, T extends IMediaItem>
		extends MediaItemList<T>
		implements IMediaItemDb<H,S,T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final boolean HIDEMISSING = true; // TODO link this to GUI?
	
	/**
	 * This pairs with escapeSearch().
	 */
	public static final String SEARCH_ESC = "\\";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaItemDbConfig config;
	private final String escapedSearchTerm;
	
	private S dbLayer;
	private IDbColumn librarySort;
	private SortDirection librarySortDirection;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO FIXME merge libraryName and searchTerm to match return value of getSerial().
	 */
	protected MediaItemDb (String listName, MediaItemDbConfig config, S dbLayer) {
		super(dbLayer.getDbFilePath(), listName);
		
		this.config = config;
		this.dbLayer = dbLayer;
		
		this.librarySort = dbLayer.getDefaultSortColumn();
		this.librarySortDirection = SortDirection.ASC;
		
		if (config.getFilter() != null) {
			this.escapedSearchTerm = escapeSearch(config.getFilter());
		}
		else {
			this.escapedSearchTerm = null;
		}
		
		try {
			readSortFromDb();
		} catch (DbException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	protected void finalize() throws Throwable {
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
	
	/* This way we only listen for DB events when someone is listening to our own events.
	 * Should help things from getting tangled during GC.  Possibly.
	 */
	
	@Override
	public void addChangeEventListener(MediaItemListChangeListener listener) {
		if (this.changeEventListeners.size() == 0) this.dbLayer.addChangeListener(this.storageChangeListener);
		super.addChangeEventListener(listener);
	}
	
	@Override
	public void removeChangeEventListener(MediaItemListChangeListener listener) {
		super.removeChangeEventListener(listener);
		if (this.changeEventListeners.size() == 0) this.dbLayer.removeChangeListener(this.storageChangeListener);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getSerial() {
		return this.config.getSerial();
	}
	
	public MediaItemDbConfig getConfig() {
		return this.config;
	}
	
	@Override
	public S getDbLayer() {
		return this.dbLayer;
	}
	
	public String getEscapedSearchTerm() {
		return this.escapedSearchTerm;
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
	
	private boolean firstRead = true;
	private long durationOfLastRead = -1;
	
	@Override
	public void read () throws MorriganException {
		if (!this.firstRead) return;
		try {
			doRead();
		} catch (DbException e) {
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
		if (this.escapedSearchTerm != null) {
			allMedia = this.dbLayer.getMedia(this.librarySort, this.librarySortDirection, HIDEMISSING, this.escapedSearchTerm, SEARCH_ESC);
		}
		else {
			allMedia = this.dbLayer.getMedia(this.librarySort, this.librarySortDirection, HIDEMISSING);
		}
		long l0 = System.currentTimeMillis() - t0;
		
		long t1 = System.currentTimeMillis();
		setMediaTracks(allMedia);
		long l1 = System.currentTimeMillis() - t1;
		
//		System.err.println("[" + l0 + "," + l1 + " ms] " + getType() + " " + getListName());
		this.durationOfLastRead = l0+l1;
		
		this.firstRead = false;
		
		this.getChangeEventCaller().mediaListRead();
	}
	
	@Override
	public long getDurationOfLastRead() {
		return this.durationOfLastRead;
	}
	
	/**
	 * FIXME Sort library list according to DB query.
	 * Clear then reload?
	 * How about loading into a new list, then replacing?
	 * Would need to be thread-safe.
	 * @throws MorriganException
	 */
	@Override
	public void forceRead () throws MorriganException {
		this.firstRead = true;
		read();
	}
	
	/**
	 * Only read if already read.
	 * No point re-reading if no one is expecting it
	 * to already be read.
	 */
	public void updateRead () throws MorriganException {
		if (!this.firstRead) {
			forceRead();
		} else {
			System.err.println("updateRead() : Skipping reRead() because its un-needed.");
		}
	}
	
	@Override
	public void commitOrRollback () throws DbException {
		this.dbLayer.commitOrRollBack();
	}
	
	@Override
	public void rollback() throws DbException {
		this.dbLayer.rollback();
	}
	
	/**
	 * Returns a copy of the main list updated with all items from the DB.
	 */
	@Override
	public List<T> getAllDbEntries () throws DbException {
		ArrayList<T> copyOfMainList = new ArrayList<T>(getMediaItems());
		List<T> allList = this.dbLayer.getAllMedia(this.dbLayer.getDefaultSortColumn(), SortDirection.ASC, false);
		updateList(copyOfMainList, allList);
		return copyOfMainList;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sorting.
	
	@Override
	public IDbColumn getSort () {
		return this.librarySort;
	}
	
	@Override
	public SortDirection getSortDirection() {
		return this.librarySortDirection;
	}
	
	@Override
	public void setSort (IDbColumn sort, SortDirection direction) throws MorriganException {
		this.librarySort = sort;
		this.librarySortDirection = direction;
		
		updateRead();
		callSortChangedListeners(this.librarySort, this.librarySortDirection);
		
		saveSortToDbInNewThread();
	}
	
	private List<SortChangeListener> _sortChangeListeners = new ArrayList<SortChangeListener>();
	
	private void callSortChangedListeners (IDbColumn sort, SortDirection direction) {
		for (SortChangeListener l : this._sortChangeListeners) {
			l.sortChanged(sort, direction);
		}
	}
	
	@Override
	public void registerSortChangeListener (SortChangeListener scl) {
		this._sortChangeListeners.add(scl);
	}
	
	@Override
	public void unregisterSortChangeListener (SortChangeListener scl) {
		this._sortChangeListeners.remove(scl);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sort saving.
	
	public static final String KEY_SORTCOL = "SORTCOL";
	public static final String KEY_SORTDIR = "SORTDIR";
	
	private void saveSortToDbInNewThread () {
		new Thread() {
			@Override
			public void run() {
				try {
					saveSortToDb();
				} catch (DbException e) {
					e.printStackTrace();
				}
			}
		}.run();
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
	public boolean hasFile (String filepath) throws MorriganException, DbException {
		return this.dbLayer.hasFile(filepath);
	}
	
	@Override
	public boolean hasFile (File file) throws MorriganException, DbException {
		return this.dbLayer.hasFile(file);
	}
	
	@Override
	public T getByFile(File file) throws DbException {
		return this.dbLayer.getByFile(file);
	}
	
	@Override
	public T getByFile (String filepath) throws DbException {
		return this.dbLayer.getByFile(filepath);
	}
	
	@Override
	public List<T> simpleSearch (String term, int maxResults) throws DbException {
		return this.dbLayer.simpleSearch(escapeSearch(term), SEARCH_ESC, maxResults);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB events.
	
	private IMediaItemStorageLayerChangeListener<T> storageChangeListener = new IMediaItemStorageLayerChangeListener<T> () {
		
		@Override
		public void propertySet(String key, String value) { /* Unused. */ }
		
		@Override
		public void mediaItemAdded(String filePath) {
			getChangeEventCaller().mediaItemsAdded((IMediaItem[])null); // TODO pass-through actual item?
		}
		
		@Override
		public void mediaItemsAdded(List<File> filePaths) {
			getChangeEventCaller().mediaItemsAdded((IMediaItem[])null); // TODO pass-through actual item?
		}
		
		@Override
		public void mediaItemRemoved(String filePath) {
			getChangeEventCaller().mediaItemsRemoved((IMediaItem[])null); // TODO pass-through actual item?
		}
		
		@Override
		public void mediaItemUpdated(String filePath) {
			getChangeEventCaller().mediaItemsUpdated((IMediaItem[])null); // TODO pass-through actual item?
		}
		
		@Override
		public void mediaItemTagAdded(IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) {
			if (MediaItemDb.this.getConfig().getFilter() != null) { // TODO make more specific?
				getChangeEventCaller().mediaItemsForceReadRequired((IMediaItem[])null); // TODO pass-through actual item?
			}
		}
		
		@Override
		public void mediaItemTagsMoved(IDbItem from_item, IDbItem to_item) {
			if (MediaItemDb.this.getConfig().getFilter() != null) { // TODO make more specific?
				getChangeEventCaller().mediaItemsForceReadRequired((IMediaItem[])null); // TODO pass-through actual item?
			}
		}
		
		@Override
		public void mediaItemTagRemoved(MediaTag tag) {
			if (MediaItemDb.this.getConfig().getFilter() != null) { // TODO make more specific?
				getChangeEventCaller().mediaItemsForceReadRequired((IMediaItem[])null); // TODO pass-through actual item?
			}
		}
		
		@Override
		public void mediaItemTagsCleared(IDbItem item) {
			if (MediaItemDb.this.getConfig().getFilter() != null) { // TODO make more specific?
				getChangeEventCaller().mediaItemsForceReadRequired((IMediaItem[])null); // TODO pass-through actual item?
			}
		}
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Updating tracks.
	
	@Override
	public void setItemDateAdded (T track, Date date) throws MorriganException {
		super.setItemDateAdded(track, date);
		try {
			this.dbLayer.setDateAdded(track.getFilepath(), date);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void removeItem (T track) throws MorriganException {
		try {
			_removeMediaTrack(track);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	/**
	 * This is so that this class can always call this method, even when this
	 * class is sub-classed and removeMediaTrack() overridden.
	 * @throws DbException 
	 */
	private void _removeMediaTrack (T track) throws MorriganException, DbException {
		super.removeItem(track);
		
		// Remove track.
		int n = this.dbLayer.removeFile(track.getFilepath());
		if (n != 1) {
			n = this.dbLayer.removeFile(track);
			if (n != 1) {
				throw new MorriganException("Failed to remove entry from DB by ROWID '"+track.getDbRowId()+"' '"+track.getFilepath()+"'.");
			}
		}
		
		// Remove tags.
		if (hasTags(track)) {
			clearTags(track);
		}
	}
	
	@Override
	public void setItemHashCode(T track, BigInteger hashcode) throws MorriganException {
		super.setItemHashCode(track, hashcode);
		try {
			this.dbLayer.setHashcode(track.getFilepath(), hashcode);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setItemDateLastModified(T track, Date date) throws MorriganException {
		super.setItemDateLastModified(track, date);
		try {
			this.dbLayer.setDateLastModified(track.getFilepath(), date);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setItemEnabled(T track, boolean value) throws MorriganException {
		super.setItemEnabled(track, value);
		try {
			this.dbLayer.setEnabled(track.getFilepath(), value);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setItemMissing(T track, boolean value) throws MorriganException {
		super.setItemMissing(track, value);
		try {
			this.dbLayer.setMissing(track.getFilepath(), value);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setRemoteLocation (T track, String remoteLocation) throws MorriganException {
		track.setRemoteLocation(remoteLocation);
		try {
			this.dbLayer.setRemoteLocation(track.getFilepath(), remoteLocation);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void persistTrackData (T track) throws MorriganException {
		try {
			this.dbLayer.setHashcode(track.getFilepath(), track.getHashcode());
    		if (track.getDateAdded() != null) this.dbLayer.setDateAdded(track.getFilepath(), track.getDateAdded());
    		if (track.getDateLastModified() != null) this.dbLayer.setDateLastModified(track.getFilepath(), track.getDateLastModified());
    		this.dbLayer.setRemoteLocation(track.getFilepath(), track.getRemoteLocation());
    		this.dbLayer.setEnabled(track.getFilepath(), track.isEnabled());
    		this.dbLayer.setMissing(track.getFilepath(), track.isMissing());
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
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void addSource (String source) throws MorriganException {
		try {
			this.dbLayer.addSource(source);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void removeSource (String source) throws MorriganException {
		try {
			this.dbLayer.removeSource(source);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Tags.
	
	@Override
	public boolean hasTags (IDbItem item) throws MorriganException {
		try {
			return this.dbLayer.hasTags(item);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public boolean hasTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws MorriganException {
		try {
			return this.dbLayer.hasTag(item, tag, type, mtc);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public List<MediaTag> getTags (IDbItem item) throws MorriganException {
		try {
			return this.dbLayer.getTags(item);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void addTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws MorriganException {
		try {
			this.dbLayer.addTag(item, tag, type, mtc);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void addTag (IDbItem item, String tag, MediaTagType type, String mtc) throws MorriganException {
		try {
			this.dbLayer.addTag(item, tag, type, mtc);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void moveTags (IDbItem from_item, IDbItem to_item) throws MorriganException {
		try {
			this.dbLayer.moveTags(from_item, to_item);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void removeTag (MediaTag mt) throws MorriganException {
		try {
			this.dbLayer.removeTag(mt);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void clearTags (IDbItem item) throws MorriganException {
		try {
			this.dbLayer.clearTags(item);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public List<MediaTagClassification> getTagClassifications() throws MorriganException {
		try {
			return this.dbLayer.getTagClassifications();
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public MediaTagClassification getTagClassification(String classificationName) throws MorriganException {
		try {
			return this.dbLayer.getTagClassification(classificationName);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void addTagClassification(String classificationName) throws MorriganException {
		try {
			this.dbLayer.addTagClassification(classificationName);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Meta tagging.
	
	private static final String TAG_UNREADABLE = "UNREADABLE";
	
	@Override
	public boolean isMarkedAsUnreadable (T mi) throws MorriganException {
		return hasTag(mi, TAG_UNREADABLE, MediaTagType.AUTOMATIC, null);
	}
	
	@Override
	public void markAsUnreadabled (T mi) throws MorriganException {
		setItemEnabled(mi, false);
		addTag(mi, TAG_UNREADABLE, MediaTagType.AUTOMATIC, (MediaTagClassificationImpl)null);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Returns object if the file was added.
	 * (i.e. it was not already in the library)
	 * @throws MorriganException 
	 * @throws DbException 
	 */
	@Override
	public T addFile (File file) throws MorriganException, DbException {
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
	 * Returns list of objects that were added.
	 * Files that were already present are ignored.
	 */
	@Override
	public List<T> addFiles (List<File> files) throws MorriganException, DbException {
		List<T> ret = new LinkedList<T>();
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
	 * This method does NOT update the in-memory model or the UI,
	 * but instead assume you are about to re-query the DB.
	 * You will want to do this to get fresh data that is sorted
	 * in the correct order.
	 * @param thereWereErrors
	 * @throws MorriganException
	 * @throws DbException 
	 */
	@Override
	public void completeBulkUpdate (boolean thereWereErrors) throws MorriganException, DbException {
		try {
			List<T> removed = replaceListWithoutSetDirty(this._changedItems);
			if (!thereWereErrors) {
				System.err.println("completeBulkUpdate() : About to clean " + removed.size() + " items...");
				for (T i : removed) {
					_removeMediaTrack(i);
				}
			}
			else {
				System.err.println("completeBulkUpdate() : Errors occured, skipping delete.");
			}
		} finally {
			this._changedItems = null;
		}
	}
	
	@Override
	public T updateItem (T mi) throws MorriganException, DbException {
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
			} else {
				throw new MorriganException("updateItem() : Failed to find item '"+mi.getFilepath()+"' in list '"+this+"'.");
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
	
	/**
	 * This pairs with SEARCH_ESC.
	 */
	static public String escapeSearch (String term) {
		String q = term.replace("'", "''");
		q = q.replace(" ", "*");
		q = q.replace("\\", "\\\\");
		q = q.replace("%", "\\%");
		q = q.replace("_", "\\_");
		q = q.replace("*", "%");
		
		return q;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
