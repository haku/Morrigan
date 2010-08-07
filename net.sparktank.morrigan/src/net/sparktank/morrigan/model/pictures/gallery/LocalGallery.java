package net.sparktank.morrigan.model.pictures.gallery;

import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItemList;
import net.sparktank.morrigan.model.MediaSqliteLayer2;
import net.sparktank.morrigan.model.MediaSqliteLayer2.DbColumn;
import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.sqlitewrapper.DbException;

/*
 * TODO FIXME Extract common code between this and AbstractMediaLibrary.
 */
public class LocalGallery extends MediaItemList<MediaPicture> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "LOCALGALLERY";
	
	public static final boolean HIDEMISSING = true; // TODO link this to GUI?
	
	private GallerySqliteLayer dbLayer;
	private DbColumn librarySort;
	private SortDirection librarySortDirection;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected LocalGallery (String libraryName, GallerySqliteLayer dbLayer) {
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
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public String getSerial() {
		return this.dbLayer.getDbFilePath();
	}
	
	protected GallerySqliteLayer getDbLayer() {
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
		List<MediaPicture> allMedia = this.dbLayer.updateListOfAllMedia(getMediaTracks(), this.librarySort, this.librarySortDirection, HIDEMISSING);
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
