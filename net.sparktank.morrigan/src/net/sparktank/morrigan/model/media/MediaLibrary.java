package net.sparktank.morrigan.model.media;

import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.library.DbConFactory;
import net.sparktank.morrigan.library.DbException;
import net.sparktank.morrigan.library.SqliteLayer;

public class MediaLibrary extends MediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private SqliteLayer dbLayer;
	private final String dbFilePath;
	
	MediaLibrary (String libraryName, String dbFilePath) throws DbException {
		super(dbFilePath, libraryName);
		this.dbFilePath = dbFilePath;
		
		dbLayer = DbConFactory.getDbLayer(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private boolean firstRead = true;
	
	@Override
	public void read () throws MorriganException {
		if (!isDirty() && !firstRead) return;
		firstRead = false;
		
		List<MediaTrack> allMedia = dbLayer.getAllMedia();
		for (MediaTrack mt : allMedia) {
			addTrack(mt);
		}
		setDirty(false);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getDbPath () {
		return dbFilePath;
	}
	
	public List<String> getSources () throws DbException {
		return dbLayer.getSources();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
