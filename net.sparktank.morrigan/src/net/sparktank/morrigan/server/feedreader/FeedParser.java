package net.sparktank.morrigan.server.feedreader;

import java.io.IOException;

import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.server.feedwriters.XmlHelper;
import net.sparktank.sqlitewrapper.DbException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Deprecated
public class FeedParser {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void parseFeed (String xmlString, TaskEventListener taskEventListener, IEntryHandler entryHandler) throws FeedParseException, DbException {
//		if (taskEventListener!=null) taskEventListener.onStart(); // TODO do this?
		if (taskEventListener!=null) taskEventListener.beginTask("Reading feed...", 100);
		
		Document doc;
		try {
			doc = XmlHelper.xmlStringToDocument(xmlString);
		} catch (SAXException e) {
			throw new FeedParseException(e);
		} catch (IOException e) {
			throw new FeedParseException(e);
		}
		
		Node feed = doc.getFirstChild(); // This should get the "feed" element.
		if (feed.getNodeName().equals("feed")) {
			NodeList entries = feed.getChildNodes();
			if (entries.getLength() < 1) {
				throw new FeedParseException("Feed element contains no entry elements.");
			}
			
			int progress = 0;
			int n = 0;
			int N = entries.getLength();
			for (int i = 0; i < entries.getLength(); i++) {
				Node entry = entries.item(i);
				if (entry.getNodeName().equals("entry")) {
					entryHandler.parseEntry(entry);
				}
				
				if (taskEventListener!=null) {
					n++;
					int p = (n * 100) / N;
					if (p > progress) {
						taskEventListener.worked(p - progress);
						progress = p;
					}
				}
			}
			
			if (taskEventListener!=null) taskEventListener.done();
			
		} else {
			throw new IllegalArgumentException("First node '"+feed.getNodeName()+"' != feed.");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
