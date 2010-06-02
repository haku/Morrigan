package net.sparktank.morrigan.server.feedreader;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.TaskEventListener;
import net.sparktank.morrigan.model.library.MediaLibraryItem;
import net.sparktank.morrigan.model.library.RemoteMediaLibrary;
import net.sparktank.morrigan.server.HttpClient;
import net.sparktank.morrigan.server.feedwriters.XmlHelper;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MediaListFeedParser2 extends DefaultHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO report progress to taskEventListener.
	 * TODO support event cancellation.
	 */
	public static void parseFeed (RemoteMediaLibrary library, TaskEventListener taskEventListener) throws MorriganException {
//		if (taskEventListener!=null) taskEventListener.onStart(); // TODO do this?
		if (taskEventListener!=null) taskEventListener.beginTask("Reading feed...", 100);
		
		// FIXME parse stream directly.
		String xmlString;
		try {
			xmlString = HttpClient.getHttpClient().doHttpRequest(library.getUrl()).getBody();
		} catch (IOException e) {
			throw new MorriganException(e);
		}
		
		try {
			library.setAutoCommit(false);
			library.beginBulkUpdate();
			
	        try {
	        	SAXParserFactory factory = SAXParserFactory.newInstance();
	        	factory.setNamespaceAware(true);
	        	factory.setValidating(true);
	        	SAXParser parser = factory.newSAXParser();
	        	parser.parse(new InputSource(new StringReader(xmlString)), new MediaListFeedParser2(library));
			}
	        catch (SAXException e) {
				throw new MorriganException(e);
			} catch (IOException e) {
				throw new MorriganException(e);
			} catch (ParserConfigurationException e) {
				throw new MorriganException(e);
			}
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
		
		if (taskEventListener!=null) taskEventListener.done();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final RemoteMediaLibrary library;
	
	private final Stack<String> stack;
	
	public MediaListFeedParser2(RemoteMediaLibrary library) {
		stack = new Stack<String>();
		this.library = library; 
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaLibraryItem currentItem;
	private StringBuilder currentText;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		stack.push(localName);
		if (stack.size() == 2 && localName.equals("entry")) {
			currentItem = new MediaLibraryItem();
		}
		else if (stack.size() == 3 && localName.equals("link")) {
			String relVal = attributes.getValue("rel");
			if (relVal != null && relVal.equals("self")) {
				String hrefVal = attributes.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					currentItem.setRemoteLocation(hrefVal);
				}
			}
		}
		
		if (currentText == null || currentText.length() > 0) {
			currentText = new StringBuilder();
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (stack.size() == 2 && localName.equals("entry")) {
			try {
				library.updateItem(currentItem);
			}
			catch (MorriganException e) {
				throw new SAXException(e);
			}
		}
		else if (stack.size() == 3 && localName.equals("title")) {
			currentItem.setFilepath(currentText.toString());
		}
		else if (stack.size() == 3 && localName.equals("duration")) {
			int v = Integer.parseInt(currentText.toString());
			currentItem.setDuration(v);
		}
		else if (stack.size() == 3 && localName.equals("hash")) {
			long v = Long.parseLong(currentText.toString());
			currentItem.setHashcode(v);
		}
		else if (stack.size() == 3 && localName.equals("startcount")) {
			long v = Long.parseLong(currentText.toString());
			currentItem.setStartCount(v);
		}
		else if (stack.size() == 3 && localName.equals("endcount")) {
			long v = Long.parseLong(currentText.toString());
			currentItem.setEndCount(v);
		}
		else if (stack.size() == 3 && localName.equals("dateadded")) {
			try {
				Date d = XmlHelper.getIso8601UtcDateFormatter().parse(currentText.toString());
				currentItem.setDateAdded(d);
			}
			catch (Exception e) {
				throw new SAXException(flattenStack(stack) + " Exception parsing date '"+currentText.toString()+"'.", e);
			}
		}
		else if (stack.size() == 3 && localName.equals("datelastmodified")) {
			try {
				Date d = XmlHelper.getIso8601UtcDateFormatter().parse(currentText.toString());
				currentItem.setDateLastModified(d);
			}
			catch (Exception e) {
				throw new SAXException(flattenStack(stack) + " Exception parsing date '"+currentText.toString()+"'.", e);
			}
		}
		else if (stack.size() == 3 && localName.equals("datelastplayed")) {
			try {
				Date d = XmlHelper.getIso8601UtcDateFormatter().parse(currentText.toString());
				currentItem.setDateLastPlayed(d);
			}
			catch (Exception e) {
				throw new SAXException(flattenStack(stack) + " Exception parsing date '"+currentText.toString()+"'.", e);
			}
		}
		
		stack.pop();
	}
	
	public void characters(char[] ch, int start, int length) {
		currentText.append( ch, start, length );
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static String flattenStack (Stack<String> stack) {
		StringBuilder sb = new StringBuilder();
		
		for (String s : stack) {
			sb.append(s);
			sb.append('/');
		}
		
		return sb.toString();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
