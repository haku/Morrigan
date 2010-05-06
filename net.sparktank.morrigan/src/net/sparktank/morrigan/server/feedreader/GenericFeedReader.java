package net.sparktank.morrigan.server.feedreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.server.feedwriters.XmlHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class GenericFeedReader {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Document doc;
	
	private List<MediaItem> mediaItemList = new ArrayList<MediaItem>();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GenericFeedReader (String xmlString) throws SAXException, IOException, FeedParseException {
		doc = XmlHelper.xmlStringToDocument(xmlString);
		Node feed = doc.getFirstChild(); // This should get the "feed" element.
		if (feed.getNodeName().equals("feed")) {
			NodeList entries = feed.getChildNodes();
			if (entries.getLength() < 1) {
				throw new FeedParseException("Feed element contains no entry elements.");
			}
			
			for (int i = 0; i < entries.getLength(); i++) {
				Node entry = entries.item(i);
				if (entry.getNodeName().equals("entry")) {
					parseEntry(entry);
				}
			}
			
		} else {
			throw new IllegalArgumentException("First node '"+feed.getNodeName()+"' != feed.");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected abstract void parseEntry (Node entry) throws FeedParseException;
	
	protected void addMediaItem (MediaItem mi) {
		mediaItemList.add(mi);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected Document getDoc () {
		return doc;
	}
	
	public List<MediaItem> getMediaItemList() {
		return mediaItemList;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
