package net.sparktank.morrigan.server.feedreader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tracks.MediaTrack;
import net.sparktank.morrigan.model.tracks.library.remote.RemoteMediaLibrary;
import net.sparktank.morrigan.server.HttpClient;
import net.sparktank.morrigan.server.HttpClient.HttpResponse;
import net.sparktank.morrigan.server.HttpClient.IHttpStreamHandler;
import net.sparktank.morrigan.server.feedwriters.XmlHelper;
import net.sparktank.sqlitewrapper.DbException;

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
				boolean thereWereErrors = true;
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
					} catch (ParserConfigurationException e) {
						throw new MorriganException(e);
					} catch (IOException e) {
						throw new MorriganException(e);
					}
					thereWereErrors = false;
				} catch (DbException e) {
					throw new MorriganException(e);
				} finally {
					try {
						library.completeBulkUpdate(thereWereErrors);
					} catch (DbException e) {
						throw new MorriganException(e);
					} finally {
						try {
							library.commit();
						} catch (DbException e) {
							throw new MorriganException(e);
						} finally {
							try {
								library.setAutoCommit(true);
							} catch (DbException e) {
								throw new MorriganException(e);
							}
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
			if (e instanceof UnknownHostException) {
				throw new MorriganException("Host unknown.", e);
			} else if (e instanceof SocketException) {
				throw new MorriganException("Host unreachable.", e);
			} else {
				throw new MorriganException(e);
			}
		}
		
		if (taskEventListener!=null) taskEventListener.done();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final RemoteMediaLibrary library;
	private final TaskEventListener taskEventListener;
	private final Stack<String> stack;
	
	public MediaListFeedParser2(RemoteMediaLibrary library, TaskEventListener taskEventListener) {
		this.taskEventListener = taskEventListener;
		this.stack = new Stack<String>();
		this.library = library; 
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long entryCount = 0;
	private long entriesProcessed = 0;
	private int progress = 0;
	private MediaTrack currentItem;
	private StringBuilder currentText;
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		this.stack.push(localName);
		if (this.stack.size() == 2 && localName.equals("entry")) {
			this.currentItem = new MediaTrack();
		}
		else if (this.stack.size() == 3 && localName.equals("link")) {
			String relVal = attributes.getValue("rel");
			if (relVal != null && relVal.equals("self")) {
				String hrefVal = attributes.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					try {
						String remotePath = URLDecoder.decode(hrefVal, "UTF-8");
						this.currentItem.setFilepath(remotePath);
						this.currentItem.setRemoteLocation(hrefVal);
					} catch (UnsupportedEncodingException e) {
						throw new SAXException(e);
					}
				}
			}
		}
		
		if (this.currentText == null || this.currentText.length() > 0) {
			this.currentText = new StringBuilder();
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (this.stack.size() == 2 && localName.equals("count")) {
			this.entryCount = Long.parseLong(this.currentText.toString());
		}
		else if (this.stack.size() == 2 && localName.equals("entry")) {
			try {
				this.library.updateItem(this.currentItem);
			}
			catch (MorriganException e) {
				throw new SAXException(e);
			} catch (DbException e) {
				throw new SAXException(e);
			}
			
			if (this.taskEventListener!=null) {
				this.entriesProcessed++;
				int p = (int) ((this.entriesProcessed * 100) / this.entryCount);
				if (p > this.progress) {
					this.taskEventListener.worked(p - this.progress);
					this.progress = p;
				}
			}
		}
//		else if (stack.size() == 3 && localName.equals("title")) {
//			currentItem.setFilepath(currentText.toString());
//		}
		else if (this.stack.size() == 3 && localName.equals("duration")) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setDuration(v);
		}
		else if (this.stack.size() == 3 && localName.equals("hash")) {
			long v = Long.parseLong(this.currentText.toString());
			this.currentItem.setHashcode(v);
		}
		else if (this.stack.size() == 3 && localName.equals("startcount")) {
			long v = Long.parseLong(this.currentText.toString());
			this.currentItem.setStartCount(v);
		}
		else if (this.stack.size() == 3 && localName.equals("endcount")) {
			long v = Long.parseLong(this.currentText.toString());
			this.currentItem.setEndCount(v);
		}
		else if (this.stack.size() == 3 && localName.equals("dateadded")) {
			try {
				Date d = XmlHelper.getIso8601UtcDateFormatter().parse(this.currentText.toString());
				this.currentItem.setDateAdded(d);
			}
			catch (Exception e) {
				throw new SAXException(flattenStack(this.stack) + " Exception parsing date '"+this.currentText.toString()+"'.", e);
			}
		}
		else if (this.stack.size() == 3 && localName.equals("datelastmodified")) {
			try {
				Date d = XmlHelper.getIso8601UtcDateFormatter().parse(this.currentText.toString());
				this.currentItem.setDateLastModified(d);
			}
			catch (Exception e) {
				throw new SAXException(flattenStack(this.stack) + " Exception parsing date '"+this.currentText.toString()+"'.", e);
			}
		}
		else if (this.stack.size() == 3 && localName.equals("datelastplayed")) {
			try {
				Date d = XmlHelper.getIso8601UtcDateFormatter().parse(this.currentText.toString());
				this.currentItem.setDateLastPlayed(d);
			}
			catch (Exception e) {
				throw new SAXException(flattenStack(this.stack) + " Exception parsing date '"+this.currentText.toString()+"'.", e);
			}
		}
		
		this.stack.pop();
	}
	
	@Override
	public void characters(char[] ch, int start, int length) {
		this.currentText.append( ch, start, length );
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
