package net.sparktank.morrigan.model.media;

import java.util.WeakHashMap;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.library.DbException;
import net.sparktank.morrigan.library.LibraryHelper;
import net.sparktank.morrigan.playlist.PlaylistHelper;

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
				}
			}
		}
		
		if (ret == null) {
			ret = new MediaLibrary(libraryName, dbFilePath);
			mediaLibraryCache.put(ret, dbFilePath);
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
				}
			}
		}
		
		if (ret == null) {
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
