package net.sparktank.morrigan.model.media;

import java.util.WeakHashMap;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.library.DbException;

public class MediaListFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static WeakHashMap<MediaLibrary, String> mediaLibraryCache = new WeakHashMap<MediaLibrary, String>();
	
	public static MediaLibrary makeMediaLibrary (String libraryName, String dbFilePath) throws DbException {
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
	
	public static MediaPlaylist makeMediaPlaylist (String filePath) throws MorriganException {
		return makeMediaPlaylist(filePath, false);
	}
	
	public static MediaPlaylist makeMediaPlaylist (String filePath, boolean newPl) throws MorriganException {
		MediaPlaylist ret = null;
		
		if (mediaPlaylistCache.containsValue(filePath)) {
			for (MediaPlaylist lib : mediaPlaylistCache.keySet()) {
				if (lib.getFilePath().equals(filePath)) {
					ret = lib;
				}
			}
		}
		
		if (ret == null) {
			ret = new MediaPlaylist(filePath, newPl);
			mediaPlaylistCache.put(ret, ret.getListId());
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
