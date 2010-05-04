package net.sparktank.morrigan.server.helpers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MediaListFeed extends GenericFeed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaListFeed (MediaList ml) throws MorriganException {
		super();
		mediaLibraryToFeed(ml, getDoc());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void mediaLibraryToFeed (MediaList ml, Document doc) throws MorriganException {
		ml.read();
		
		String listFile;
		try {
			listFile = URLEncoder.encode(filenameFromPath(ml.getListId()), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		Node feed = doc.getFirstChild(); // This should get the "feed" element.
		addElement(doc, feed, "title", ml.getListName());
		addLink(doc, feed, "/media/" + ml.getType() + "/" + listFile, "self");
		addLink(doc, feed, "/media/" + ml.getType() + "/" + listFile + "/src", "src");
		addLink(doc, feed, "/media/" + ml.getType() + "/" + listFile + "/scan", "scan");
		addLink(doc, feed, "/player/0/play/" + listFile, "play"); // FIXME list all players here.
		
		for (MediaItem mi : ml.getMediaTracks()) {
			Element entry = doc.createElement("entry");
			
			String file;
			try {
				file = URLEncoder.encode(filenameFromPath(mi.getFilepath()), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			
			addElement(doc, entry, "title", mi.getTitle());
			addLink(doc, entry, "/media/" + ml.getType() + "/" + filenameFromPath(ml.getListId()) + "/" + file);
			addLink(doc, entry, "/player/0/play/" + listFile + "/" + file, "play"); // FIXME list all players here.
			addElement(doc, entry, "duration", mi.getDuration());
			addElement(doc, entry, "hash", mi.getHashcode());
			addElement(doc, entry, "startcount", mi.getStartCount());
			addElement(doc, entry, "endcount", mi.getEndCount());
			
			feed.appendChild(entry);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
