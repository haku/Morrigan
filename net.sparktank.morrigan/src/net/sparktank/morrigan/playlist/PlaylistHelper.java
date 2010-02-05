package net.sparktank.morrigan.playlist;

import java.io.File;
import java.util.ArrayList;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.MediaListFactory;
import net.sparktank.morrigan.model.media.MediaPlaylist;
import net.sparktank.morrigan.model.ui.MediaExplorerItem;

public class PlaylistHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static String getPathForNewPlaylist (String plName) {
		File plDir = Config.getPlDir();
		String plFile = plDir.getPath() + File.separator + plName + Config.PL_FILE_EXT;
		return plFile;
	}
	
	public static MediaPlaylist createPl (String plName) throws MorriganException {
		String plFile = getPathForNewPlaylist(plName);
		MediaPlaylist pl = MediaListFactory.makeMediaPlaylist(PlaylistHelper.getPlaylistTitle(plFile), plFile, true);
		pl.read();
		return pl;
	}
	
	private static boolean isPlFile (String filePath) {
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
