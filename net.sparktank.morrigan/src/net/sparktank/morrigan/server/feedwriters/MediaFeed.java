package net.sparktank.morrigan.server.feedwriters;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.library.local.LocalLibraryHelper;
import net.sparktank.morrigan.model.playlist.PlaylistHelper;
import net.sparktank.morrigan.player.Player;
import net.sparktank.morrigan.player.PlayerRegister;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;

public class MediaFeed extends Feed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaFeed () {
		super();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected void populateFeed(DataWriter dw) throws SAXException, MorriganException {
		addElement(dw, "title", "Morrigan media desu~");
		addLink(dw, "/media" , "self", "text/xml");
		addLink(dw, "/media/newlib", "newlib", "cmd");
		
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
				dw.startElement("entry");

				addElement(dw, "title", i.title);
				addLink(dw, "/media/" + type + "/" + fileName, "self", "text/xml");
				
				for (Player p : players) {
					addLink(dw, "/player/" + p.getId() + "/play/" + fileName, "play", "cmd");
				}
				
				dw.endElement("entry");
			}
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
