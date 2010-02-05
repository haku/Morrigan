package net.sparktank.morrigan.model.media;

import java.io.File;
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
		setCanBeDirty(false);
		
		this.dbFilePath = dbFilePath;
		
		dbLayer = DbConFactory.getDbLayer(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private boolean firstRead = true;
	
	@Override
	public void read () throws MorriganException {
		if (!firstRead) return;
		firstRead = false;
		
		List<MediaTrack> allMedia = dbLayer.getAllMedia();
		for (MediaTrack mt : allMedia) {
			addTrack(mt);
		}
	}
	
	@Override
	public void addTrack(MediaTrack track) {
		super.addTrack(track);
		setDirty(false);
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
	
	public boolean addFile (File file) throws DbException {
		boolean added = dbLayer.addFile(file);
		if (added) addTrack(new MediaTrack(file.getAbsolutePath()));
		return added;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
