package net.sparktank.morrigan.model;

import java.util.WeakHashMap;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.library.DbConFactory;
import net.sparktank.morrigan.model.library.DbException;
import net.sparktank.morrigan.model.library.LibraryHelper;
import net.sparktank.morrigan.model.library.MediaLibrary;
import net.sparktank.morrigan.model.library.RemoteMediaLibrary;
import net.sparktank.morrigan.model.playlist.MediaPlaylist;
import net.sparktank.morrigan.model.playlist.PlaylistHelper;

public class MediaListFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static WeakHashMap<MediaLibrary, String> mediaLibraryCache = new WeakHashMap<MediaLibrary, String>();
	
	public static synchronized MediaLibrary makeMediaLibrary (String dbFilePath) throws DbException {
		return makeMediaLibrary(LibraryHelper.getLibraryTitle(dbFilePath), dbFilePath);
	}
	
	public static synchronized MediaLibrary makeMediaLibrary (String libraryName, String dbFilePath) throws DbException {
		MediaLibrary ret = null;
		
		if (mediaLibraryCache.containsValue(dbFilePath)) {
			for (MediaLibrary lib : mediaLibraryCache.keySet()) {
				if (lib.getDbPath().equals(dbFilePath)) {
					ret = lib;
					System.out.println("Found '" + dbFilePath + "' in cache.");
				}
			}
		}
		
		if (ret == null) {
			System.out.println("Making object instance '" + dbFilePath + "'...");
			ret = new MediaLibrary(libraryName, DbConFactory.getDbLayer(dbFilePath));
			mediaLibraryCache.put(ret, dbFilePath);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static WeakHashMap<RemoteMediaLibrary, String> remoteMediaLibraryCache = new WeakHashMap<RemoteMediaLibrary, String>();
	
	public static RemoteMediaLibrary makeRemoteMediaLibrary(String dbFilePath) throws DbException {
		return makeRemoteMediaLibrary(LibraryHelper.getLibraryTitle(dbFilePath), null, dbFilePath);
	}
	
	public static RemoteMediaLibrary makeRemoteMediaLibrary(String libraryName, String serverUrl, String dbFilePath) throws DbException {
		RemoteMediaLibrary ret = null;
		
		if (remoteMediaLibraryCache.containsValue(dbFilePath)) {
			for (RemoteMediaLibrary lib : remoteMediaLibraryCache.keySet()) {
				if (lib.getDbPath().equals(dbFilePath)) {
					ret = lib;
					System.out.println("Found '" + dbFilePath + "' in cache.");
				}
			}
		}
		
		if (ret == null) {
			System.out.println("Making object instance '" + dbFilePath + "'...");
			if (serverUrl != null) {
				ret = new RemoteMediaLibrary(libraryName, serverUrl, DbConFactory.getDbLayer(dbFilePath));
			} else {
				ret = new RemoteMediaLibrary(libraryName, DbConFactory.getDbLayer(dbFilePath));
			}
			remoteMediaLibraryCache.put(ret, dbFilePath);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static WeakHashMap<MediaPlaylist, String> mediaPlaylistCache = new WeakHashMap<MediaPlaylist, String>();
	
	public static synchronized MediaPlaylist makeMediaPlaylist (String filePath) throws MorriganException {
		return makeMediaPlaylist(PlaylistHelper.getPlaylistTitle(filePath), filePath, false);
	}
	
	public static synchronized MediaPlaylist makeMediaPlaylist (String title, String filePath) throws MorriganException {
		return makeMediaPlaylist(title, filePath, false);
	}
	
	public static MediaPlaylist makeMediaPlaylist (String title, String filePath, boolean newPl) throws MorriganException {
		MediaPlaylist ret = null;
		
		if (mediaPlaylistCache.containsValue(filePath)) {
			for (MediaPlaylist lst : mediaPlaylistCache.keySet()) {
				if (lst.getFilePath().equals(filePath)) {
					ret = lst;
					System.out.println("Found '" + filePath + "' in cache.");
				}
			}
		}
		
		if (ret == null) {
			System.out.println("Making object instance '" + filePath + "'...");
			ret = new MediaPlaylist(title, filePath, newPl);
			mediaPlaylistCache.put(ret, ret.getListId());
		}
		
		return ret;
	}
	
	public static void finalisePlaylists () {
		for (MediaPlaylist lst : mediaPlaylistCache.keySet()) {
			try {
				lst.clean();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
