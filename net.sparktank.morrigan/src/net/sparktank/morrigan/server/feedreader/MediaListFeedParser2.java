package net.sparktank.morrigan.server.feedreader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import net.sparktank.morrigan.server.HttpClient.HttpResponse;
import net.sparktank.morrigan.server.HttpClient.IHttpStreamHandler;
import net.sparktank.morrigan.server.feedwriters.XmlHelper;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MediaListFeedParser2 extends DefaultHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO report progress to taskEventListener.
	 * TODO support event cancellation.
	 */
	public static void parseFeed (final RemoteMediaLibrary library, final TaskEventListener taskEventListener) throws MorriganException {
//		if (taskEventListener!=null) taskEventListener.onStart(); // TODO do this?
		if (taskEventListener!=null) taskEventListener.beginTask("Reading feed...", 100);
		
		IHttpStreamHandler httpStreamHandler = new IHttpStreamHandler () {
			@Override
			public void handleStream(InputStream is) throws MorriganException {
				try {
					library.setAutoCommit(false);
					library.beginBulkUpdate();
			        try {
			        	SAXParserFactory factory = SAXParserFactory.newInstance();
			        	factory.setNamespaceAware(true);
			        	factory.setValidating(true);
			        	SAXParser parser = factory.newSAXParser();
			        	parser.parse(is, new MediaListFeedParser2(library, taskEventListener));
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
			}
		};
		
		try {
			HttpResponse response = HttpClient.getHttpClient().doHttpRequest(library.getUrl(), httpStreamHandler);
			if (response.getCode() != 200) {
				throw new MorriganException("After fetching remote library response code was " + response.getCode() + " (expected 200).");
			}
			
		} catch (IOException e) {
			throw new MorriganException(e);
		}
		
		if (taskEventListener!=null) taskEventListener.done();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final RemoteMediaLibrary library;
	private final TaskEventListener taskEventListener;
	private final Stack<String> stack;
	
	public MediaListFeedParser2(RemoteMediaLibrary library, TaskEventListener taskEventListener) {
		this.taskEventListener = taskEventListener;
		stack = new Stack<String>();
		this.library = library; 
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long entryCount = 0;
	private long entriesProcessed = 0;
	private int progress = 0;
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
					try {
						String remotePath = URLDecoder.decode(hrefVal, "UTF-8");
						currentItem.setFilepath(remotePath);
						currentItem.setRemoteLocation(remotePath); // FIXME is remoteLocation being used anywhere?
					} catch (UnsupportedEncodingException e) {
						throw new SAXException(e);
					}
				}
			}
		}
		
		if (currentText == null || currentText.length() > 0) {
			currentText = new StringBuilder();
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (stack.size() == 2 && localName.equals("count")) {
			entryCount = Long.parseLong(currentText.toString());
		}
		else if (stack.size() == 2 && localName.equals("entry")) {
			try {
				library.updateItem(currentItem);
			}
			catch (MorriganException e) {
				throw new SAXException(e);
			}
			
			if (taskEventListener!=null) {
				entriesProcessed++;
				int p = (int) ((entriesProcessed * 100) / entryCount);
				if (p > progress) {
					taskEventListener.worked(p - progress);
					progress = p;
				}
			}
		}
//		else if (stack.size() == 3 && localName.equals("title")) {
//			currentItem.setFilepath(currentText.toString());
//		}
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
