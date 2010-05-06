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
				
				System.err.println("node '" + item.getNodeName() + "' has att:");
				for (int j = 0; j < att.getLength(); j++) {
					System.err.println("   " + att.item(j).getNodeName() + "=" + att.item(j).getNodeValue());
				}
				
				Node relNode = att.getNamedItem("rel");
				if (relNode != null) {
					String relVal = relNode.getNodeValue();
					if (relVal != null && relVal.equals("self")) {
						Node hrefNode = att.getNamedItem("href");
						if (hrefNode != null) {
							String hrefVal = hrefNode.getNodeValue();
							if (hrefVal != null) {
								mi.setFilepath(hrefVal);
							}
						} else {
							throw new FeedParseException("Link missing 'href' att.");
						}
					}
				} else {
					throw new FeedParseException("Link missing 'rel' att.");
				}
				
			} else if (item.getNodeName().equals("title")) {
				mi.setDisplayTitle(item.getTextContent());
				
			} else if (item.getNodeName().equals("duration")) {
				int v = Integer.parseInt(item.getTextContent());
				mi.setDuration(v);
				
			} else if (item.getNodeName().equals("hash")) {
				long v = Long.parseLong(item.getTextContent());
				mi.setHashcode(v);
				
			} else if (item.getNodeName().equals("startcount")) {
				long v = Long.parseLong(item.getTextContent());
				mi.setStartCount(v);
				
			} else if (item.getNodeName().equals("endcount")) {
				long v = Long.parseLong(item.getTextContent());
				mi.setEndCount(v);
			}
			
		}
		
		addMediaItem(mi);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
