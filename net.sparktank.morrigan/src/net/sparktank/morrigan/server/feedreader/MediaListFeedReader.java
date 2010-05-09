package net.sparktank.morrigan.server.feedreader;

import java.io.IOException;
import java.util.Date;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.TaskEventListener;
import net.sparktank.morrigan.model.library.MediaLibraryItem;
import net.sparktank.morrigan.model.library.RemoteMediaLibrary;
import net.sparktank.morrigan.server.HttpClient;
import net.sparktank.morrigan.server.feedwriters.XmlHelper;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MediaListFeedReader extends GenericFeedReader {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final RemoteMediaLibrary library;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaListFeedReader(RemoteMediaLibrary library, TaskEventListener taskEventListener) throws SAXException, IOException, MorriganException {
		super(HttpClient.getHttpClient().doHttpRequest(library.getUrl()).getBody(), taskEventListener);
		this.library = library;
		
		try {
			library.setAutoCommit(false);
			library.beginBulkUpdate();
			parse();
		} finally {
			try {
				library.completeBulkUpdate();
			} finally {
				try {
					library.commit();
				} finally {
					library.setAutoCommit(true);
				}
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void parseEntry(Node entry) throws FeedParseException {
		NodeList childNodes = entry.getChildNodes();
		if (childNodes.getLength() < 1) {
			throw new FeedParseException("Entry contains no elements.");
		}
		
		MediaLibraryItem mi = new MediaLibraryItem();
		
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node item = childNodes.item(i);
			
			if (item.getNodeName().equals("link")) {
				NamedNodeMap att = item.getAttributes();
				
				Node relNode = att.getNamedItem("rel");
				if (relNode != null) {
					String relVal = relNode.getNodeValue();
					if (relVal != null && relVal.equals("self")) {
						Node hrefNode = att.getNamedItem("href");
						if (hrefNode != null) {
							String hrefVal = hrefNode.getNodeValue();
							if (hrefVal != null) {
								mi.setRemoteLocation(hrefVal);
							}
						} else {
							throw new FeedParseException("Link missing 'href' att.");
						}
					}
				} else {
					throw new FeedParseException("Link missing 'rel' att.");
				}
				
			} else if (item.getNodeName().equals("title")) {
				mi.setFilepath(item.getTextContent());
				
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
				
			} else if (item.getNodeName().equals("dateadded")) {
				try {
					Date d = XmlHelper.getIso8601UtcDateFormatter().parse(item.getTextContent());
					mi.setDateAdded(d);
					
				} catch (Exception e) {
					throw new FeedParseException("Exception parsing date '"+item.getTextContent()+"'.", e);
				}
				
			} else if (item.getNodeName().equals("datelastmodified")) {
				try {
					Date d = XmlHelper.getIso8601UtcDateFormatter().parse(item.getTextContent());
					mi.setDateLastModified(d);
					
				} catch (Exception e) {
					throw new FeedParseException("Exception parsing date '"+item.getTextContent()+"'.", e);
				}
				
			} else if (item.getNodeName().equals("datelastplayed")) {
				try {
					Date d = XmlHelper.getIso8601UtcDateFormatter().parse(item.getTextContent());
					mi.setDateLastPlayed(d);
					
				} catch (Exception e) {
					throw new FeedParseException("Exception parsing date '"+item.getTextContent()+"'.", e);
				}
			}
			
		}
		
		try {
			library.updateItem(mi);
		} catch (MorriganException e) {
			throw new FeedParseException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
