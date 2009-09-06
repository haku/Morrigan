package net.sparktank.morrigan.helpers;

import java.io.File;
import java.util.ArrayList;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.model.media.MediaPlaylist;
import net.sparktank.morrigan.model.ui.MediaExplorerItem;

public class PlaylistHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static PlaylistHelper instance = new PlaylistHelper();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getPathForNewPlaylist () {
		// TODO
		return null;
	}
	
	public boolean isPlFile (String filePath) {
		return (filePath.toLowerCase().endsWith(Config.PL_FILE_EXT));
	}
	
	public ArrayList<MediaExplorerItem> getAllPlaylists () {
		ArrayList<MediaExplorerItem> ret = new ArrayList<MediaExplorerItem>();
		
		// TODO better display name / show icons for PLs?
		
		File plDir = Config.getPlDir();
		File [] plFiles = plDir.listFiles();
		
		// empty dir?
		if (plFiles == null ) return ret;
		
		for (File file : plFiles) {
			if (isPlFile(file.getAbsolutePath())) {
				System.out.println("found pl = " + file);
				
				MediaExplorerItem newItem = new MediaExplorerItem(MediaExplorerItem.ItemType.PLAYLIST);
				newItem.identifier = file.getAbsolutePath();
				
				int x = newItem.identifier.lastIndexOf(File.separator);
				if (x > 0) {
					newItem.title = newItem.identifier.substring(x+1);
				} else {
					newItem.title = newItem.identifier;
				}
				
				ret.add(newItem);
			}
		}
		
		return ret;
	}
	
	public void showEditorForPlaylist (MediaPlaylist mPlaylist) {
		// TODO
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
