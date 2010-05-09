package net.sparktank.morrigan.server.feedreader;

import java.io.IOException;

import net.sparktank.morrigan.model.TaskEventListener;
import net.sparktank.morrigan.server.feedwriters.XmlHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class GenericFeedReader {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Document doc;
	private final TaskEventListener taskEventListener;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GenericFeedReader (String xmlString, TaskEventListener taskEventListener) throws SAXException, IOException {
		this.taskEventListener = taskEventListener;
		doc = XmlHelper.xmlStringToDocument(xmlString);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected void parse() throws FeedParseException {
//		if (taskEventListener!=null) taskEventListener.onStart(); // TODO do this?
		if (taskEventListener!=null) taskEventListener.beginTask("Reading feed...", 100);
		
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
					parseEntry(entry);
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
	
	protected abstract void parseEntry (Node entry) throws FeedParseException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected Document getDoc () {
		return doc;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
