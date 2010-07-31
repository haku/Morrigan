package net.sparktank.morrigan.model.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaTrack;
import net.sparktank.morrigan.model.MediaTrackList;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySort;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySortDirection;
import net.sparktank.morrigan.model.tags.MediaTag;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;
import net.sparktank.sqlitewrapper.DbException;

public abstract class AbstractMediaLibrary extends MediaTrackList<MediaLibraryTrack> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final boolean HIDEMISSING = true; // TODO link this to GUI?
	
	private SqliteLayer dbLayer;
	private LibrarySort librarySort;
	private LibrarySortDirection librarySortDirection;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected AbstractMediaLibrary (String libraryName, SqliteLayer dbLayer) {
		super(dbLayer.getDbFilePath(), libraryName);
		this.dbLayer = dbLayer;
		
		this.librarySort = LibrarySort.FILE;
		this.librarySortDirection = LibrarySortDirection.ASC;
	}
	
	@Override
	protected void finalize() throws Throwable {
		dbLayer.dispose();
		super.finalize();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	abstract public String getType();
	
	@Override
	public String getSerial() {
		return dbLayer.getDbFilePath();
	}
	
	protected SqliteLayer getDbLayer() {
		return dbLayer;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean isCanBeDirty () {
		return false;
	}
	
	public boolean allowDuplicateEntries () {
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private boolean firstRead = true;
	private long durationOfLastRead = -1;
	
	@Override
	public void read () throws MorriganException {
		if (!firstRead) return;
		try {
			doRead();
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	protected void doRead () throws MorriganException, DbException {
		System.err.println("[?] reading... " + getType() + " " + getListName() + "...");
		
		long t0 = System.currentTimeMillis();
		List<MediaLibraryTrack> allMedia = dbLayer.updateListOfAllMedia(getMediaTracks(), librarySort, librarySortDirection, HIDEMISSING);
		long l0 = System.currentTimeMillis() - t0;
		
		long t1 = System.currentTimeMillis();
		setMediaTracks(allMedia);
		long l1 = System.currentTimeMillis() - t1;
		
		System.err.println("[" + l0 + "," + l1 + " ms] " + getType() + " " + getListName());
		durationOfLastRead = l0+l1;
		
		firstRead = false;
	}
	
	/**
	 * FIXME Sort library list according to DB query.
	 * Clear then reload?
	 * How about loading into a new list, then replacing?
	 * Would need to be thread-safe.
	 * @throws MorriganException
	 */
	public void reRead () throws MorriganException {
		firstRead = true;
		read();
	}
	
	/**
	 * Only read if already read.
	 * No point re-reading if no one is expecting it
	 * to already be read.
	 */
	public void updateRead () throws MorriganException {
		if (!firstRead) {
			reRead();
		} else {
			System.err.println("updateRead() : Skipping reRead() because its un-needed.");
		}
	}
	
	public void setAutoCommit (boolean b) throws DbException {
		dbLayer.setAutoCommit(b);
	}
	
	public void commit () throws DbException {
		dbLayer.commit();
	}
	
	public void rollback () throws DbException {
		dbLayer.rollback();
	}
	
	/**
	 * Returns a copy of the main list updated with all items from the DB.
	 */
	public List<MediaLibraryTrack> getAllLibraryEntries () throws DbException {
		ArrayList<MediaLibraryTrack> copyOfMainList = new ArrayList<MediaLibraryTrack>(getMediaTracks());
		List<MediaLibraryTrack> allList = dbLayer.getAllMedia(LibrarySort.FILE, LibrarySortDirection.ASC, false);
		updateList(copyOfMainList, allList);
		return copyOfMainList;
	}
	
	public long getDurationOfLastRead() {
		return durationOfLastRead;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sorting.
	
	public LibrarySort getSort () {
		return librarySort;
	}
	
	public LibrarySortDirection getSortDirection() {
		return librarySortDirection;
	}
	
	public void setSort (LibrarySort sort, LibrarySortDirection direction) throws MorriganException {
		librarySort = sort;
		librarySortDirection = direction;
		updateRead();
		callSortChangedListeners(librarySort, librarySortDirection);
	}
	
	private List<SortChangeListener> _sortChangeListeners = new ArrayList<SortChangeListener>();
	
	private void callSortChangedListeners (LibrarySort sort, LibrarySortDirection direction) {
		for (SortChangeListener l : _sortChangeListeners) {
			l.sortChanged(sort, direction);
		}
	}
	
	public void registerSortChangeListener (SortChangeListener scl) {
		_sortChangeListeners.add(scl);
	}
	
	public void unregisterSortChangeListener (SortChangeListener scl) {
		_sortChangeListeners.remove(scl);
	}
	
	public interface SortChangeListener {
		public void sortChanged (LibrarySort sort, LibrarySortDirection direction);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public List<MediaLibraryTrack> simpleSearch (String term, String esc, int maxResults) throws DbException {
		return dbLayer.simpleSearch(term, esc, maxResults);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void incTrackStartCnt (MediaTrack track, long n) throws MorriganException {
		super.incTrackStartCnt(track, n);
		try {
			dbLayer.incTrackStartCnt(track.getFilepath(), n);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void incTrackEndCnt (MediaTrack track, long n) throws MorriganException {
		super.incTrackEndCnt(track, n);
		try {
			dbLayer.incTrackEndCnt(track.getFilepath(), n);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setDateAdded (MediaLibraryTrack track, Date date) throws MorriganException {
		super.setDateAdded(track, date);
		try {
			dbLayer.setDateAdded(track.getFilepath(), date);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setDateLastPlayed (MediaLibraryTrack track, Date date) throws MorriganException {
		super.setDateLastPlayed(track, date);
		try {
			dbLayer.setDateLastPlayed(track.getFilepath(), date);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void removeMediaTrack (MediaLibraryTrack track) throws MorriganException {
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
	private void _removeMediaTrack (MediaLibraryTrack track) throws MorriganException, DbException {
		super.removeMediaTrack(track);
		
		// Remove track.
		int n = dbLayer.removeFile(track.getFilepath());
		if (n != 1) {
			if (track instanceof MediaLibraryTrack) {
				MediaLibraryTrack mli = (MediaLibraryTrack) track;
				n = dbLayer.removeFile(mli.getDbRowId());
				if (n != 1) {
					throw new MorriganException("Failed to remove entry from DB by ROWID '"+mli.getDbRowId()+"' '"+track.getFilepath()+"'.");
				}
				
			} else {
				throw new MorriganException("Failed to remove entry from DB '"+track.getFilepath()+"'.");
			}
		}
		
		// Remove tags.
		if (hasTags(track)) {
			clearTags(track);
		}
	}
	
	@Override
	public void incTrackStartCnt(MediaTrack track) throws MorriganException {
		super.incTrackStartCnt(track);
		try {
			dbLayer.incTrackPlayed(track.getFilepath());
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void incTrackEndCnt(MediaTrack track) throws MorriganException {
		super.incTrackEndCnt(track);
		try {
			dbLayer.incTrackFinished(track.getFilepath());
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setTrackDuration(MediaTrack track, int duration) throws MorriganException {
		super.setTrackDuration(track, duration);
		try {
			dbLayer.setTrackDuration(track.getFilepath(), duration);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setTrackHashCode(MediaLibraryTrack track, long hashcode) throws MorriganException {
		super.setTrackHashCode(track, hashcode);
		try {
			dbLayer.setHashcode(track.getFilepath(), hashcode);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setTrackDateLastModified(MediaLibraryTrack track, Date date) throws MorriganException {
		super.setTrackDateLastModified(track, date);
		try {
			dbLayer.setDateLastModified(track.getFilepath(), date);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setTrackEnabled(MediaLibraryTrack track, boolean value) throws MorriganException {
		super.setTrackEnabled(track, value);
		try {
			dbLayer.setEnabled(track.getFilepath(), value);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setTrackMissing(MediaLibraryTrack track, boolean value) throws MorriganException {
		super.setTrackMissing(track, value);
		try {
			dbLayer.setMissing(track.getFilepath(), value);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	public void setRemoteLocation (MediaLibraryTrack track, String remoteLocation) throws DbException {
		track.setRemoteLocation(remoteLocation);
		dbLayer.setRemoteLocation(track.getFilepath(), remoteLocation);
	}
	
	public void persistTrackData (MediaLibraryTrack track) throws DbException {
		dbLayer.setTrackDuration(track.getFilepath(), track.getDuration());
		dbLayer.setHashcode(track.getFilepath(), track.getHashcode());
		dbLayer.setTrackStartCnt(track.getFilepath(), track.getStartCount());
		dbLayer.setTrackEndCnt(track.getFilepath(), track.getEndCount());
		if (track.getDateAdded() != null) dbLayer.setDateAdded(track.getFilepath(), track.getDateAdded());
		if (track.getDateLastModified() != null) dbLayer.setDateLastModified(track.getFilepath(), track.getDateLastModified());
		if (track.getDateLastPlayed() != null) dbLayer.setDateLastPlayed(track.getFilepath(), track.getDateLastPlayed());
		dbLayer.setRemoteLocation(track.getFilepath(), track.getRemoteLocation());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getDbPath () {
		return dbLayer.getDbFilePath();
	}
	
	public List<String> getSources () throws DbException {
		return dbLayer.getSources();
	}
	
	public void addSource (String source) throws DbException {
		dbLayer.addSource(source);
	}
	
	public void removeSource (String source) throws DbException {
		dbLayer.removeSource(source);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Tags.
	
	public boolean hasTags (MediaLibraryTrack mlt) throws MorriganException {
		try {
			return dbLayer.hasTags(mlt.getDbRowId());
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	public boolean hasTag (MediaLibraryTrack mlt, String tag, MediaTagType type, MediaTagClassification mtc) throws MorriganException {
		try {
			return dbLayer.hasTag(mlt.getDbRowId(), tag, type, mtc);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	public List<MediaTag> getTags (MediaLibraryTrack mlt) throws MorriganException {
		try {
			return dbLayer.getTags(mlt.getDbRowId());
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	public void addTag (MediaLibraryTrack mlt, String tag, MediaTagType type, MediaTagClassification mtc) throws MorriganException {
		try {
			dbLayer.addTag(mlt.getDbRowId(), tag, type, mtc);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	public void addTag (MediaLibraryTrack mlt, String tag, MediaTagType type, String mtc) throws MorriganException {
		try {
			dbLayer.addTag(mlt.getDbRowId(), tag, type, mtc);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	public void moveTags (MediaLibraryTrack from_mlt, MediaLibraryTrack to_mlt) throws MorriganException {
		try {
			dbLayer.moveTags(from_mlt.getDbRowId(), to_mlt.getDbRowId());
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	public void removeTag (MediaTag mt) throws MorriganException {
		try {
			dbLayer.removeTag(mt);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	public void clearTags (MediaLibraryTrack mlt) throws MorriganException {
		try {
			dbLayer.clearTags(mlt.getDbRowId());
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Meta tagging.
	
	private static final String TAG_UNREADABLE = "UNREADABLE";
	
	public boolean isMarkedAsUnreadable (MediaLibraryTrack mi) throws MorriganException {
		return hasTag(mi, TAG_UNREADABLE, MediaTagType.AUTOMATIC, null);
	}
	
	public void markAsUnreadabled (MediaLibraryTrack mi) throws MorriganException {
		setTrackEnabled(mi, false);
		addTag(mi, TAG_UNREADABLE, MediaTagType.AUTOMATIC, (MediaTagClassification)null);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Returns true if the file was added.
	 * (i.e. it was not already in the library)
	 * @throws DbException 
	 */
	public MediaLibraryTrack addFile (File file) throws MorriganException, DbException {
		MediaLibraryTrack track = null;
		boolean added = dbLayer.addFile(file);
		if (added) {
			track = new MediaLibraryTrack(file.getAbsolutePath());
			addTrack(track);
		}
		return track;
	}
	
	private List<MediaLibraryTrack> _changedItems = null;
	
	public void beginBulkUpdate () {
		if (_changedItems != null) throw new IllegalArgumentException("beginBulkUpdate() : Build update alredy in progress.");
		_changedItems = new ArrayList<MediaLibraryTrack>();
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
			List<MediaLibraryTrack> removed = replaceListWithoutSetDirty(_changedItems);
			if (!thereWereErrors) {
				System.err.println("completeBulkUpdate() : About to clean " + removed.size() + " items...");
				for (MediaLibraryTrack i : removed) {
					_removeMediaTrack(i);
				}
			}
			else {
				System.err.println("completeBulkUpdate() : Errors occured, skipping delete.");
			}
		} finally {
			_changedItems = null;
		}
	}
	
	public void updateItem (MediaLibraryTrack mi) throws MorriganException, DbException {
		if (_changedItems == null) {
			throw new IllegalArgumentException("updateItem() can only be called after beginBulkUpdate() and before completeBulkUpdate().");
		}
		
		MediaLibraryTrack track = null;
		
		boolean added = dbLayer.addFile(mi.getFilepath(), -1);
		if (added) {
			track = mi;
			addTrack(track);
			persistTrackData(track);
			
		} else {
			// Update item.
			List<MediaLibraryTrack> mediaTracks = getMediaTracks();
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
		
		_changedItems.add(mi);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
