package net.sparktank.morrigan.server.feedwriters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MediaListFeed<T extends MediaList<? extends MediaItem>> extends GenericFeed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaListFeed (T ml) throws MorriganException {
		super();
		mediaLibraryToFeed(ml, getDoc());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void mediaLibraryToFeed (T ml, Document doc) throws MorriganException {
		ml.read();
		
		String listFile;
		try {
			listFile = URLEncoder.encode(filenameFromPath(ml.getListId()), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		Node feed = doc.getFirstChild(); // This should get the "feed" element.
		addElement(doc, feed, "title", ml.getListName());
		addLink(doc, feed, "/media/" + ml.getType() + "/" + listFile, "self", "text/xml");
		addLink(doc, feed, "/media/" + ml.getType() + "/" + listFile + "/src", "src", "text/xml");
		addLink(doc, feed, "/media/" + ml.getType() + "/" + listFile + "/scan", "scan", "cmd");
		addLink(doc, feed, "/player/0/play/" + listFile, "play", "cmd"); // FIXME list all players here.
		
		for (MediaItem mi : ml.getMediaTracks()) {
			Element entry = doc.createElement("entry");
			
			String file;
			try {
				file = URLEncoder.encode(mi.getFilepath(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			
			addElement(doc, entry, "title", mi.getTitle());
			addLink(doc, entry, "/media/" + ml.getType() + "/" + filenameFromPath(ml.getListId()) + "/" + file, "self", "text/xml");
			addLink(doc, entry, "/player/0/play/" + listFile + "/" + file, "play", "cmd"); // FIXME list all players here.
			addElement(doc, entry, "duration", mi.getDuration());
			addElement(doc, entry, "hash", mi.getHashcode());
			addElement(doc, entry, "startcount", mi.getStartCount());
			addElement(doc, entry, "endcount", mi.getEndCount());
			if (mi.getDateAdded() != null) {
				addElement(doc, entry, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
			}
			if (mi.getDateLastModified() != null) {
				addElement(doc, entry, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
			}
			if (mi.getDateLastPlayed() != null) {
				addElement(doc, entry, "datelastplayed", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastPlayed()));
			}
			
			feed.appendChild(entry);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
