package net.sparktank.morrigan.model.tracks.playlist;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;

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
		@SuppressWarnings("boxing")
		MediaPlaylist pl = MediaPlaylist.FACTORY.manufacture(plFile, true);
		pl.read();
		return pl;
	}
	
	public static boolean isPlFile (String filePath) {
		return (filePath.toLowerCase().endsWith(Config.PL_FILE_EXT));
	}
	
	public static ArrayList<MediaExplorerItem> getAllPlaylists () {
		ArrayList<MediaExplorerItem> ret = new ArrayList<MediaExplorerItem>();
		
		File plDir = Config.getPlDir();
		File [] plFiles = plDir.listFiles();
		
		// empty dir?
		if (plFiles == null || plFiles.length < 1 ) return ret;
		
		for (File file : plFiles) {
			if (isPlFile(file.getAbsolutePath())) {
				MediaExplorerItem newItem = new MediaExplorerItem(MediaExplorerItem.ItemType.PLAYLIST);
				newItem.identifier = file.getAbsolutePath();
				newItem.title = getPlaylistTitle(newItem.identifier);
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
