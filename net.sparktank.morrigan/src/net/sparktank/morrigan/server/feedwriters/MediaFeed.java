package net.sparktank.morrigan.server.feedwriters;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.library.LocalLibraryHelper;
import net.sparktank.morrigan.model.playlist.PlaylistHelper;
import net.sparktank.morrigan.player.Player;
import net.sparktank.morrigan.player.PlayerRegister;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MediaFeed extends GenericFeed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaFeed () {
		super();
		mediaListsToFeed(getDoc());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void mediaListsToFeed(Document doc) {
		Node feed = doc.getFirstChild(); // This should get the "feed" element.
		addElement(doc, feed, "title", "Morrigan media desu~");
		addLink(doc, feed, "/media" , "self");
		addLink(doc, feed, "/media/newlib", "newlib");
			
		List<Player> players = PlayerRegister.getPlayers();
		
		for (int n = 0; n < 2; n++) {
			ArrayList<MediaExplorerItem> items = new ArrayList<MediaExplorerItem>();
			String type = null;
			
			switch (n) {
				case 0:
					type="library";
					items.addAll(LocalLibraryHelper.getAllLibraries());
					break;
				
				case 1:
					type="playlist";
					items.addAll(PlaylistHelper.getAllPlaylists());
					break;
				
			}
			
			for (MediaExplorerItem i : items) {
				String fileName = i.identifier.substring(i.identifier.lastIndexOf(File.separator) + 1);
				
				Element entry = doc.createElement("entry");
				addElement(doc, entry, "title", i.title);
				addLink(doc, entry, "/media/" + type + "/" + fileName);
				
				for (Player p : players) {
					addLink(doc, entry, "/player/" + p.getId() + "/play/" + fileName, "play");
				}
				
				feed.appendChild(entry);
			}
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
