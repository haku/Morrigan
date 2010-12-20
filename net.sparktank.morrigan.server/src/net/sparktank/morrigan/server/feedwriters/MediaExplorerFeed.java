package net.sparktank.morrigan.server.feedwriters;
import java.io.File;
import java.util.List;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.MediaListReference;
import net.sparktank.morrigan.model.media.impl.MediaFactoryImpl;
import net.sparktank.morrigan.player.IPlayerLocal;
import net.sparktank.morrigan.player.PlayerRegister;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;

public class MediaExplorerFeed extends AbstractFeed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory.
	
	static private final ThreadLocal<MediaExplorerFeed> FACTORY = new ThreadLocal<MediaExplorerFeed>() {
		@Override
		protected MediaExplorerFeed initialValue() {
			return new MediaExplorerFeed();
		}
	};
	
	static public MediaExplorerFeed getInsance () {
		return FACTORY.get();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaExplorerFeed () {
		super();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void populateFeed(DataWriter dw) throws SAXException, MorriganException {
		addElement(dw, "title", "Morrigan media desu~");
		addLink(dw, "/media" , "self", "text/xml");
		addLink(dw, "/media/newmmdb", "newmmdb", "cmd");
		addLink(dw, "/media/newlib", "newlib", "cmd");
		
		List<IPlayerLocal> players = PlayerRegister.getLocalPlayers();
		
		for (MediaListReference i : MediaFactoryImpl.get().getAllLocalMixedMediaDbs()) {
			String fileName = i.getIdentifier().substring(i.getIdentifier().lastIndexOf(File.separator) + 1);
			dw.startElement("entry");
			
			addElement(dw, "title", i.getTitle());
			addLink(dw, "/media/" + ILocalMixedMediaDb.TYPE + "/" + fileName, "self", "text/xml");
			
			for (IPlayerLocal p : players) {
				addLink(dw, "/player/" + p.getId() + "/play/" + fileName, "play", "cmd");
			}
			
			dw.endElement("entry");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
