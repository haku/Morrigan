package com.vaguehope.morrigan.player;

import java.util.LinkedList;
import java.util.List;

import com.vaguehope.sqlitewrapper.DbException;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMediaTrackDb;
import net.sparktank.morrigan.model.media.MediaListReference;
import net.sparktank.morrigan.model.media.impl.MediaFactoryImpl;

public class PlayerHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO Make this method able to take all sorts if user input.
	 */
	static public List<PlayItem> queryForPlayableItems (String query1, String query2, int maxResults) throws MorriganException {
		List<PlayItem> ret = new LinkedList<PlayItem>();
		
		List<MediaListReference> items = new LinkedList<MediaListReference>();
		List<MediaListReference> matches = new LinkedList<MediaListReference>();
		items.addAll(MediaFactoryImpl.get().getAllLocalMixedMediaDbs());
		for (MediaListReference i : items) {
			if (i.getTitle().contains(query1) || query1.contains(i.getTitle()) ) {
				matches.add(i);
			}
		}
		
		for (MediaListReference explorerItem : matches) {
			if (ret.size() >= maxResults) break;
			
			/*
			 * FIXME this will load the DB (if its not already loaded), which is excessive if we are
			 * just going to show some search results.
			 */
			IMediaTrackDb<?, ?, ? extends IMediaTrack> db = mediaListReferenceToReadTrackDb(explorerItem);
			
    		if (query2 == null) {
    			ret.add(new PlayItem(db, null));
    		}
    		else {
    			List<? extends IMediaTrack> results;
    			results = runQueryOnList(db, query2, maxResults);
    			
    			for (IMediaTrack result : results) {
    				if (ret.size() >= maxResults) break;
    				ret.add(new PlayItem(db, result));
    			}
    		}
			
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public IMediaTrackDb<?, ?, ? extends IMediaTrack> mediaListReferenceToReadTrackDb (MediaListReference item) throws MorriganException {
		IMediaTrackDb<?, ?, ? extends IMediaTrack> ret = null;
		
		if (item.getType() == MediaListReference.MediaListType.LOCALMMDB) {
			ILocalMixedMediaDb mmdb;
			try {
				mmdb = MediaFactoryImpl.get().getLocalMixedMediaDb(item.getIdentifier());
			} catch (DbException e) {
				throw new MorriganException(e);
			}
			mmdb.read();
			ret = mmdb;
		}
		else {
			throw new MorriganException("TODO: show " + item.getIdentifier());
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * What is the point in this???
	 */
	static public List<? extends IMediaTrack> runQueryOnList (IMediaTrackDb<?,?,? extends IMediaTrack> mediaDb, String query, int maxResults) throws MorriganException {
		List<? extends IMediaTrack> res;
		try {
			res = mediaDb.simpleSearch(query, maxResults);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
		
		return res;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
