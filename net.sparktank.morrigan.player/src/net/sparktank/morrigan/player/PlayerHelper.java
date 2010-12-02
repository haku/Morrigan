package net.sparktank.morrigan.player;

import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMediaTrackDb;
import net.sparktank.morrigan.model.media.impl.MediaFactoryImpl;
import net.sparktank.sqlitewrapper.DbException;

public class PlayerHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO Make this method able to take all sorts if user input.
	 */
	static public List<PlayItem> queryForPlayableItems (String query1, String query2, int maxResults) throws MorriganException {
		List<PlayItem> ret = new LinkedList<PlayItem>();
		
		List<MediaExplorerItem> items = new LinkedList<MediaExplorerItem>();
		List<MediaExplorerItem> matches = new LinkedList<MediaExplorerItem>();
		items.addAll(MediaFactoryImpl.get().getAllLocalMixedMediaDbs());
		for (MediaExplorerItem i : items) {
			if (i.title.contains(query1) || query1.contains(i.title) ) {
				matches.add(i);
			}
		}
		
		for (MediaExplorerItem explorerItem : matches) {
			if (ret.size() >= maxResults) break;
			
			/*
			 * FIXME this will load the DB (if its not already loaded), which is excessive if we are
			 * just going to show some search results.
			 */
			IMediaTrackDb<?, ?, ? extends IMediaTrack> db = mediaExporerItemToReadTrackDb(explorerItem);
			
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
	
	static public IMediaTrackDb<?, ?, ? extends IMediaTrack> mediaExporerItemToReadTrackDb (MediaExplorerItem item) throws MorriganException {
		IMediaTrackDb<?, ?, ? extends IMediaTrack> ret = null;
		
		if (item.type == MediaExplorerItem.ItemType.LOCALMMDB) {
			ILocalMixedMediaDb mmdb;
			try {
				mmdb = MediaFactoryImpl.get().getLocalMixedMediaDb(item.identifier);
			} catch (DbException e) {
				throw new MorriganException(e);
			}
			mmdb.read();
			ret = mmdb;
		}
		else {
			throw new MorriganException("TODO: show " + item.identifier);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public List<? extends IMediaTrack> runQueryOnList (IMediaTrackDb<?,?,? extends IMediaTrack> mediaDb, String query, int maxResults) throws MorriganException {
		String q = MediaFactoryImpl.get().escapeSearch(query);
		
		List<? extends IMediaTrack> res;
		try {
			res = mediaDb.simpleSearch(q, maxResults);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
		
		return res;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
