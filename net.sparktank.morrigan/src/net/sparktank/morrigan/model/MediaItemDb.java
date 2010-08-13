package net.sparktank.morrigan.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.morrigan.model.db.interfaces.IDbItem;
import net.sparktank.morrigan.model.media.impl.MediaItemList;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemStorageLayer;
import net.sparktank.morrigan.model.tags.MediaTag;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;
import net.sparktank.sqlitewrapper.DbException;

public abstract class MediaItemDb<S extends IMediaItemStorageLayer<T>, T extends IMediaItem> extends MediaItemList<T> implements IMediaItemDb<S,T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final boolean HIDEMISSING = true; // TODO link this to GUI?
	
	private S dbLayer;
	private IDbColumn librarySort;
	private SortDirection librarySortDirection;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected MediaItemDb (String libraryName, S dbLayer) {
		super(dbLayer.getDbFilePath(), libraryName);
		this.dbLayer = dbLayer;
		
		this.librarySort = MediaSqliteLayer2.SQL_TBL_MEDIAFILES_COL_FILE;
		this.librarySortDirection = SortDirection.ASC;
	}
	
	@Override
	protected void finalize() throws Throwable {
		this.dbLayer.dispose();
		super.finalize();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected abstract T getNewT (String filePath);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getSerial() {
		return this.dbLayer.getDbFilePath();
	}
	
	@Override
	public S getDbLayer() {
		return this.dbLayer;
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
		System.err.println("[?] reading... " + getType() + " " + getListName() + "...");
		
		long t0 = System.currentTimeMillis();
		List<T> allMedia = this.dbLayer.updateListOfAllMedia(getMediaItems(), this.librarySort, this.librarySortDirection, HIDEMISSING);
		long l0 = System.currentTimeMillis() - t0;
		
		long t1 = System.currentTimeMillis();
		setMediaTracks(allMedia);
		long l1 = System.currentTimeMillis() - t1;
		
		System.err.println("[" + l0 + "," + l1 + " ms] " + getType() + " " + getListName());
		this.durationOfLastRead = l0+l1;
		
		this.firstRead = false;
	}
	
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
	public void reRead () throws MorriganException {
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
			reRead();
		} else {
			System.err.println("updateRead() : Skipping reRead() because its un-needed.");
		}
	}
	
	public void setAutoCommit (boolean b) throws DbException {
		this.dbLayer.setAutoCommit(b);
	}
	
	public void commit () throws DbException {
		this.dbLayer.commit();
	}
	
	public void rollback () throws DbException {
		this.dbLayer.rollback();
	}
	
	/**
	 * Returns a copy of the main list updated with all items from the DB.
	 */
	public List<T> getAllLibraryEntries () throws DbException {
		ArrayList<T> copyOfMainList = new ArrayList<T>(getMediaItems());
		List<T> allList = this.dbLayer.getAllMedia(MediaSqliteLayer2.SQL_TBL_MEDIAFILES_COL_FILE, SortDirection.ASC, false);
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
	
	@Override
	public List<T> simpleSearch (String term, String esc, int maxResults) throws DbException {
		return this.dbLayer.simpleSearch(term, esc, maxResults);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
	public void setItemHashCode(T track, long hashcode) throws MorriganException {
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
	
	public void setRemoteLocation (T track, String remoteLocation) throws DbException {
		track.setRemoteLocation(remoteLocation);
		this.dbLayer.setRemoteLocation(track.getFilepath(), remoteLocation);
	}
	
	public void persistTrackData (T track) throws DbException {
		this.dbLayer.setHashcode(track.getFilepath(), track.getHashcode());
		if (track.getDateAdded() != null) this.dbLayer.setDateAdded(track.getFilepath(), track.getDateAdded());
		if (track.getDateLastModified() != null) this.dbLayer.setDateLastModified(track.getFilepath(), track.getDateLastModified());
		this.dbLayer.setRemoteLocation(track.getFilepath(), track.getRemoteLocation());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getDbPath () {
		return this.dbLayer.getDbFilePath();
	}
	
	@Override
	public List<String> getSources () throws DbException {
		return this.dbLayer.getSources();
	}
	
	@Override
	public void addSource (String source) throws DbException {
		this.dbLayer.addSource(source);
	}
	
	@Override
	public void removeSource (String source) throws DbException {
		this.dbLayer.removeSource(source);
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Meta tagging.
	
	private static final String TAG_UNREADABLE = "UNREADABLE";
	
	public boolean isMarkedAsUnreadable (T mi) throws MorriganException {
		return hasTag(mi, TAG_UNREADABLE, MediaTagType.AUTOMATIC, null);
	}
	
	public void markAsUnreadabled (T mi) throws MorriganException {
		setItemEnabled(mi, false);
		addTag(mi, TAG_UNREADABLE, MediaTagType.AUTOMATIC, (MediaTagClassification)null);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Returns true if the file was added.
	 * (i.e. it was not already in the library)
	 * @throws MorriganException 
	 * @throws DbException 
	 */
	public T addFile (File file) throws MorriganException, DbException {
		T track = null;
		boolean added = this.dbLayer.addFile(file);
		if (added) {
			track = getNewT(file.getAbsolutePath());
			addItem(track);
		}
		return track;
	}
	
	private List<T> _changedItems = null;
	
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
	
	public void updateItem (T mi) throws MorriganException, DbException {
		if (this._changedItems == null) {
			throw new IllegalArgumentException("updateItem() can only be called after beginBulkUpdate() and before completeBulkUpdate().");
		}
		
		T track = null;
		
		boolean added = this.dbLayer.addFile(mi.getFilepath(), -1);
		if (added) {
			track = mi;
			addItem(track);
			persistTrackData(track);
			
		} else {
			// Update item.
			List<T> mediaTracks = getMediaItems();
			int index = mediaTracks.indexOf(mi);
			if (index >= 0) {
				track = mediaTracks.get(index);
			} else {
				throw new MorriganException("updateItem() : Failed to find item '"+mi.getFilepath()+"' in list '"+this+"'.");
			}
			if (track.setFromMediaItem(mi)) {
				setDirtyState(DirtyState.DIRTY); // just to trigger change events.
				persistTrackData(track);
			}
		}
		
		this._changedItems.add(mi);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
