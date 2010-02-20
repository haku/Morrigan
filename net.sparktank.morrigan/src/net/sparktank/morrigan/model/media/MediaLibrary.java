package net.sparktank.morrigan.model.media;

import java.io.File;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.library.DbConFactory;
import net.sparktank.morrigan.library.DbException;
import net.sparktank.morrigan.library.SqliteLayer;
import net.sparktank.morrigan.library.SqliteLayer.LibrarySort;
import net.sparktank.morrigan.library.SqliteLayer.LibrarySortDirection;

public class MediaLibrary extends MediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "LIBRARY";
	
	private SqliteLayer dbLayer;
	private final String dbFilePath;
	LibrarySort librarySort;
	LibrarySortDirection librarySortDirection;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	MediaLibrary (String libraryName, String dbFilePath) throws DbException {
		super(dbFilePath, libraryName);
		
		this.dbFilePath = dbFilePath;
		this.librarySort = LibrarySort.FILE;
		this.librarySortDirection = LibrarySortDirection.ASC;
		
		dbLayer = DbConFactory.getDbLayer(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public String getSerial() {
		return dbFilePath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean isCanBeDirty () {
		return false;
	}
	
	public boolean allowDuplicateEntries () {
		return false;
	}
	
	private boolean firstRead = true;
	
	@Override
	public void read () throws MorriganException {
		if (!firstRead) return;
		firstRead = false;
		
		List<MediaItem> allMedia = dbLayer.getAllMedia(librarySort, librarySortDirection);
		replaceList(allMedia);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
	
	public LibrarySort getSort () {
		return librarySort;
	}
	
	public void setSort (LibrarySort sort, LibrarySortDirection direction) throws MorriganException {
		librarySort = sort;
		librarySortDirection = direction;
		reRead();
	}
	
	@Override
	protected void replaceList(List<MediaItem> mediaTracks) {
		super.replaceList(mediaTracks);
	}
	
	@Override
	public void addTrack(MediaItem track) {
		super.addTrack(track);
	}
	
	@Override
	public void incTrackStartCnt(MediaItem track) throws MorriganException {
		super.incTrackStartCnt(track);
		dbLayer.incTrackStartCnt(track.getFilepath());
	}
	
	@Override
	public void incTrackEndCnt(MediaItem track) throws MorriganException {
		super.incTrackEndCnt(track);
		dbLayer.incTrackEndCnt(track.getFilepath());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getDbPath () {
		return dbFilePath;
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
