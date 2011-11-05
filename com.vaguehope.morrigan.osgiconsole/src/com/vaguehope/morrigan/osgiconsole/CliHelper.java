package com.vaguehope.morrigan.osgiconsole;

import java.util.LinkedList;
import java.util.List;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackDb;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.impl.MediaFactoryImpl;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbFactory;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbHelper;
import com.vaguehope.sqlitewrapper.DbException;

public class CliHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO Make this method able to take all sorts if user input.
	 */
	static public List<PlayItem> queryForPlayableItems (String query1, String query2, int maxResults) throws MorriganException {
		List<PlayItem> ret = new LinkedList<PlayItem>();
		
		List<MediaListReference> items = new LinkedList<MediaListReference>();
		List<MediaListReference> matches = new LinkedList<MediaListReference>();
		
		items.addAll(MediaFactoryImpl.get().getAllLocalMixedMediaDbs());
		items.addAll(RemoteMixedMediaDbHelper.getAllRemoteMmdb());
		
		for (MediaListReference i : items) {
			if (i.getTitle().contains(query1)) {
				matches.add(i);
			}
		}
		
		for (MediaListReference explorerItem : matches) {
			if (ret.size() >= maxResults) break;
			
			/*
			 * FIXME this will load the DB (if its not already loaded), which is excessive if we are
			 * just going to show some search results.
			 */
			IMediaTrackDb<?,? extends IMediaTrack> db = mediaListReferenceToReadTrackDb(explorerItem);
			
			if (query2 == null) {
				ret.add(new PlayItem(db, null));
			}
			else {
				List<? extends IMediaTrack> results;
				try {
					results = db.simpleSearch(query2, maxResults);
				} catch (DbException e) { throw new MorriganException(e); }
				
				for (IMediaTrack result : results) {
					if (ret.size() >= maxResults) break;
					ret.add(new PlayItem(db, result));
				}
			}
			
		}
		
		return ret;
	}
	
	static public IMediaTrackDb<?,? extends IMediaTrack> mediaListReferenceToReadTrackDb (MediaListReference item) throws MorriganException {
		IMediaTrackDb<?,? extends IMediaTrack> ret = null;
		
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
		else if (item.getType() == MediaListType.REMOTEMMDB) {
			IRemoteMixedMediaDb db = RemoteMixedMediaDbFactory.getExisting(item.getIdentifier());
			db.read();
			ret = db;
		}
		else {
			throw new MorriganException("TODO: show " + item.getIdentifier());
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
