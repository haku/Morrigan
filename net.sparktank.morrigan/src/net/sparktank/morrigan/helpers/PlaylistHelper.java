package net.sparktank.morrigan.helpers;

import java.io.File;
import java.util.ArrayList;

import net.sparktank.morrigan.model.media.MediaPlaylist;
import net.sparktank.morrigan.model.ui.MediaExplorerItem;
import net.sparktank.morrigan.model.ui.MenuItem;

public class PlaylistHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static PlaylistHelper instance = new PlaylistHelper();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getPathForNewPlaylist () {
		// TODO
		return null;
	}
	
	public ArrayList<MediaExplorerItem> getAllPlaylists () {
		// TODO
		// FIXME
		
		ArrayList<MediaExplorerItem> ret = new ArrayList<MediaExplorerItem>();
		for (int i = 0; i < 10; i++) {
			MediaExplorerItem newItem = new MediaExplorerItem(MediaExplorerItem.ItemType.PLAYLIST);
			
			newItem.identifier = "\\path\\playlists\\playlist" + i;
			
			int x = newItem.identifier.lastIndexOf(File.separator);
			if (x > 0) {
				newItem.title = newItem.identifier.substring(x+1);
			} else {
				newItem.title = newItem.identifier;
			}
			
			ret.add(newItem);
		}
		return ret;
	}
	
	public void showEditorForPlaylist (MediaPlaylist mPlaylist) {
		// TODO
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
