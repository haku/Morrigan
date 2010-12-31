package net.sparktank.morrigan.android.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sparktank.morrigan.android.model.Artifact;
import net.sparktank.morrigan.android.model.ArtifactList;
import net.sparktank.morrigan.android.model.PlayerQueue;
import net.sparktank.morrigan.android.model.PlayerReference;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class PlayerQueueImpl implements PlayerQueue, ContentHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String ENTRY = "entry";
	public static final String TITLE = "title";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final List<Artifact> artifactList = new LinkedList<Artifact>();
	private final PlayerReference playerReference;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PlayerQueueImpl (InputStream dataIs, PlayerReference playerReference) throws SAXException {
		this.playerReference = playerReference;
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp;
		try {
			sp = spf.newSAXParser();
			XMLReader xmlReader = sp.getXMLReader();
			xmlReader.setContentHandler(this);
			try {
				xmlReader.parse(new InputSource(dataIs));
			}
			catch (IOException e) {
				throw new SAXException(e);
			}
		}
		catch (ParserConfigurationException e) {
			throw new SAXException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public List<? extends Artifact> getArtifactList() {
		return Collections.unmodifiableList(this.artifactList);
	}
	
//	@Override
//	public List<? extends MlistItem> getQueueItemList() {
//		return Collections.unmodifiableList(this.mlistItemList);
//	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Stack<String> stack = new Stack<String>();
	private StringBuilder currentText;
	
	private String currentTitle = null;
	private String currentListRelativeUrl = null;
	private String currentItemRelativeUrl = null;
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		this.stack.push(localName);
		
		if (this.stack.size() == 2 && localName.equals(ENTRY)) {
			this.currentTitle = null;
			this.currentListRelativeUrl = null;
			this.currentItemRelativeUrl = null;
		}
		else if (this.stack.size() == 3 && localName.equals("link")) {
			String relVal = attributes.getValue("rel");
			if (relVal != null && relVal.equals("item")) {
				String hrefVal = attributes.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					this.currentItemRelativeUrl = hrefVal;
				}
			}
			else if (relVal != null && relVal.equals("list")) {
				String hrefVal = attributes.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					this.currentListRelativeUrl = hrefVal;
				}
			}
		}
		
		// If we need a new StringBuilder, make one.
		if (this.currentText == null || this.currentText.length() > 0) {
			this.currentText = new StringBuilder();
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (this.stack.size() == 2 && localName.equals(ENTRY)) {
			if (this.currentItemRelativeUrl != null) {
				MlistItemBasicImpl item = new MlistItemBasicImpl();
				item.setTitle(this.currentTitle);
				item.setRelativeUrl(this.currentItemRelativeUrl);
				item.setType(1); // TODO reference an enum?
				this.artifactList.add(item);
			}
			else {
				MlistStateBasicImpl list = new MlistStateBasicImpl();
				list.setTitle(this.currentTitle);
				list.setBaseUrl(this.playerReference.getServerReference().getBaseUrl() + this.currentListRelativeUrl);
				
				this.artifactList.add(list);
			}
		}
		else if (this.stack.size() == 3 && localName.equals(TITLE)) {
			this.currentTitle = this.currentText.toString();
		}
		
		this.stack.pop();
	}
	
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
        this.currentText.append( ch, start, length );
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void endDocument() throws SAXException { /* UNUSED */ }
	@Override
	public void endPrefixMapping(String prefix) throws SAXException { /* UNUSED */ }
	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException { /* UNUSED */ }
	@Override
	public void processingInstruction(String target, String data) throws SAXException { /* UNUSED */ }
	@Override
	public void setDocumentLocator(Locator locator) { /* UNUSED */ }
	@Override
	public void skippedEntity(String name) throws SAXException { /* UNUSED */ }
	@Override
	public void startDocument() throws SAXException { /* UNUSED */ }
	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException { /* UNUSED */ }
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getSortKey() {
		return ""; // This should never be relevant.
	}
	
	@Override
	public int compareTo(ArtifactList another) {
		return this.getSortKey().compareTo(another.getSortKey());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
