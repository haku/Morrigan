package net.sparktank.morrigan.server.feedreader;

import java.io.IOException;
import java.net.URL;

import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.server.HttpClient;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MediaListFeedReader extends GenericFeedReader {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaListFeedReader(URL url) throws SAXException, IOException, FeedParseException {
		super(HttpClient.getHttpClient().doHttpRequest(url).getBody());
	}
	
	public MediaListFeedReader(String xmlString) throws SAXException, IOException, FeedParseException {
		super(xmlString);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void parseEntry(Node entry) throws FeedParseException {
		NodeList childNodes = entry.getChildNodes();
		if (childNodes.getLength() < 1) {
			throw new FeedParseException("Entry contains no elements.");
		}
		
		MediaItem mi = new MediaItem();
		
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node item = childNodes.item(i);
			
			if (item.getNodeName().equals("link")) {
				NamedNodeMap att = item.getAttributes();
				Node relNode = att.getNamedItem("rel");
				String relVal = relNode.getNodeValue();
				if (relVal != null) {
					if (relVal.equals("self")) {
						mi.setFilepath(relVal);
					}
				}
				
			} else if (item.getNodeName().equals("title")) {
				mi.setDisplayTitle(item.getNodeValue());
				
			} else if (item.getNodeName().equals("duration")) {
				int v = Integer.parseInt(item.getNodeValue());
				mi.setDuration(v);
				
			} else if (item.getNodeName().equals("hash")) {
				long v = Long.parseLong(item.getNodeValue());
				mi.setHashcode(v);
				
			} else if (item.getNodeName().equals("startcount")) {
				long v = Long.parseLong(item.getNodeValue());
				mi.setStartCount(v);
				
			} else if (item.getNodeName().equals("endcount")) {
				long v = Long.parseLong(item.getNodeValue());
				mi.setEndCount(v);
			}
			
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
