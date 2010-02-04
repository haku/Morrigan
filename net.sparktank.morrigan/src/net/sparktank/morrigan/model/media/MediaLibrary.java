package net.sparktank.morrigan.model.media;

import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.library.DbConFactory;
import net.sparktank.morrigan.library.DbException;
import net.sparktank.morrigan.library.SqliteLayer;

public class MediaLibrary extends MediaList {
	
	private SqliteLayer dbLayer;
	
	public MediaLibrary (String libraryName, String dbFilePath) throws DbException {
		super(dbFilePath, libraryName);
		
		dbLayer = DbConFactory.getDbLayer(dbFilePath);
	}
	
	@Override
	public void read () throws MorriganException {
		List<MediaTrack> allMedia = dbLayer.getAllMedia();
		
		for (MediaTrack mt : allMedia) {
			addTrack(mt);
		}
	}
	
}
