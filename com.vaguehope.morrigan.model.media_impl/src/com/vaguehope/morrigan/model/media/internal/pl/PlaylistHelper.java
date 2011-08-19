package com.vaguehope.morrigan.model.media.internal.pl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.internal.MediaListReferenceImpl;

import net.sparktank.morrigan.config.Config;

public class PlaylistHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static String getFullPathToPlaylist (String fileName) {
		File plDir = Config.getPlDir();
		String plFile = plDir.getPath() + File.separator + fileName;
		
		if (!plFile.toLowerCase().endsWith(Config.PL_FILE_EXT)) {
			plFile = plFile.concat(Config.PL_FILE_EXT);
		}
		
		return plFile;
	}
	
	public static MediaPlaylist createPl (String plName) throws MorriganException {
		String plFile = getFullPathToPlaylist(plName);
		MediaPlaylist pl = MediaPlaylistFactory.INSTANCE.manufacture(plFile, true);
		pl.read();
		return pl;
	}
	
	public static boolean isPlFile (String filePath) {
		return (filePath.toLowerCase().endsWith(Config.PL_FILE_EXT));
	}
	
	public static ArrayList<MediaListReference> getAllPlaylists () {
		ArrayList<MediaListReference> ret = new ArrayList<MediaListReference>();
		
		File plDir = Config.getPlDir();
		File [] plFiles = plDir.listFiles();
		
		// empty dir?
		if (plFiles == null || plFiles.length < 1 ) return ret;
		
		for (File file : plFiles) {
			String absolutePath = file.getAbsolutePath();
			if (isPlFile(absolutePath)) {
				MediaListReference newItem = new MediaListReferenceImpl(MediaListReference.MediaListType.PLAYLIST, absolutePath, getPlaylistTitle(absolutePath));
				ret.add(newItem);
			}
		}
		
		Collections.sort(ret);
		
		return ret;
	}
	
	public static String getPlaylistTitle (String filePath) {
		String ret = filePath;
		int x;
		
		x = ret.lastIndexOf(File.separator);
		if (x > 0) {
			ret = ret.substring(x+1);
		}
		
		x = ret.lastIndexOf(Config.PL_FILE_EXT);
		if (x > 0) {
			ret = ret.substring(0, x);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
