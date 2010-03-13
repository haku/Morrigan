package net.sparktank.morrigan.model.media;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.library.DbException;
import net.sparktank.morrigan.library.SqliteLayer;
import net.sparktank.morrigan.library.SqliteLayer.LibrarySort;
import net.sparktank.morrigan.library.SqliteLayer.LibrarySortDirection;

public class MediaLibrary extends MediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "LIBRARY";
	
	public static final boolean HIDEMISSING = true; // TODO like this to GUI?
	
	private SqliteLayer dbLayer;
	LibrarySort librarySort;
	LibrarySortDirection librarySortDirection;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	MediaLibrary (String libraryName, SqliteLayer dbLayer) throws DbException {
		super(dbLayer.getDbFilePath(), libraryName);
		this.dbLayer = dbLayer;
		
		this.librarySort = LibrarySort.FILE;
		this.librarySortDirection = LibrarySortDirection.ASC;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public String getSerial() {
		return dbLayer.getDbFilePath();
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
	
	@Override
	public void read () throws MorriganException {
		if (!firstRead) return;
		firstRead = false;
		
		List<MediaItem> allMedia = dbLayer.getAllMedia(librarySort, librarySortDirection, HIDEMISSING);
		replaceList(allMedia);
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
		reRead();
		callSortChangedListeners(sort, direction);
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
	
	@Override
	public void incTrackStartCnt (MediaItem track, long n) throws MorriganException {
		super.incTrackStartCnt(track, n);
		dbLayer.incTrackStartCnt(track.getFilepath(), n);
	}
	
	@Override
	public void incTrackEndCnt (MediaItem track, long n) throws MorriganException {
		super.incTrackEndCnt(track, n);
		dbLayer.incTrackEndCnt(track.getFilepath(), n);
	}
	
	@Override
	public void setDateAdded (MediaItem track, Date date) throws MorriganException {
		super.setDateAdded(track, date);
		dbLayer.setDateAdded(track.getFilepath(), date);
	}
	
	@Override
	public void setDateLastPlayed (MediaItem track, Date date) throws MorriganException {
		super.setDateLastPlayed(track, date);
		dbLayer.setDateLastPlayed(track.getFilepath(), date);
	}
	
	@Override
	public void removeMediaTrack (MediaItem track) throws MorriganException {
		super.removeMediaTrack(track);
		dbLayer.removeFile(track.getFilepath());
	}
	
	@Override
	protected void replaceList(List<MediaItem> mediaTracks) {
		super.replaceList(mediaTracks);
	}
	
	@Override
	public void incTrackStartCnt(MediaItem track) throws MorriganException {
		super.incTrackStartCnt(track);
		dbLayer.incTrackPlayed(track.getFilepath());
	}
	
	@Override
	public void incTrackEndCnt(MediaItem track) throws MorriganException {
		super.incTrackEndCnt(track);
		dbLayer.incTrackFinished(track.getFilepath());
	}
	
	@Override
	public void setTrackDuration(MediaItem track, int duration) throws MorriganException {
		super.setTrackDuration(track, duration);
		dbLayer.setTrackDuration(track.getFilepath(), duration);
	}
	
	@Override
	public void setTrackHashCode(MediaItem track, long hashcode) throws MorriganException {
		super.setTrackHashCode(track, hashcode);
		dbLayer.setHashcode(track.getFilepath(), hashcode);
	}
	
	@Override
	public void setTrackEnabled(MediaItem track, boolean value) throws MorriganException {
		super.setTrackEnabled(track, value);
		dbLayer.setEnabled(track.getFilepath(), value);
	}
	
	@Override
	public void setTrackMissing(MediaItem track, boolean value) throws MorriganException {
		super.setTrackMissing(track, value);
		dbLayer.setMissing(track.getFilepath(), value);
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
	
	/**
	 * Returns true if the file was added.
	 * (i.e. it was not already in the library)
	 */
	public boolean addFile (File file) throws MorriganException {
		boolean added = dbLayer.addFile(file);
		if (added) addTrack(new MediaItem(file.getAbsolutePath()));
		return added;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
