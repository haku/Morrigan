/*
 * Copyright 2010 Alex Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package net.sparktank.morrigan.android.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sparktank.morrigan.android.model.Artifact;
import net.sparktank.morrigan.android.model.ArtifactList;
import net.sparktank.morrigan.android.model.MlistItem;
import net.sparktank.morrigan.android.model.MlistItemList;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class MlistItemListImpl implements MlistItemList, ContentHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String ENTRY = "entry";
	
	public static final String TITLE = "title";
	public static final String TYPE = "type";
	public static final String DURATION = "duration";
	public static final String STARTCOUNT = "startcount";
	public static final String ENDCOUNT = "endcount";
	public static final String HASHCODE = "hash";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final List<MlistItem> mlistItemList = new LinkedList<MlistItem>();
	
	private final String query;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MlistItemListImpl (InputStream dataIs, String query) throws SAXException {
		this.query = query;
		
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
	public List<? extends MlistItem> getMlistItemList() {
		return Collections.unmodifiableList(this.mlistItemList);
	}
	
	@Override
	public List<? extends Artifact> getArtifactList() {
		return Collections.unmodifiableList(this.mlistItemList);
	}
	
	@Override
	public String getQuery() {
		return this.query;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Stack<String> stack = new Stack<String>();
	private StringBuilder currentText;
	private MlistItemBasicImpl currentItem;
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		this.stack.push(localName);
		
		if (this.stack.size() == 2 && localName.equals(ENTRY)) {
			this.currentItem = new MlistItemBasicImpl();
		}
		else if (this.stack.size() == 3 && localName.equals("link")) {
			String relVal = attributes.getValue("rel");
			if (relVal != null && relVal.equals("self")) {
				String hrefVal = attributes.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					this.currentItem.setRelativeUrl(hrefVal);
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
			this.mlistItemList.add(this.currentItem);
			this.currentItem = null;
		}
		else if (this.stack.size() == 3 && localName.equals(TITLE)) {
			this.currentItem.setTrackTitle(this.currentText.toString());
		}
		else if (this.stack.size() == 3 && localName.equals(TYPE)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setType(v);
		}
		else if (this.stack.size() == 3 && localName.equals(DURATION)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setDuration(v);
		}
		else if (this.stack.size() == 3 && localName.equals(STARTCOUNT)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setStartCount(v);
		}
		else if (this.stack.size() == 3 && localName.equals(ENDCOUNT)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setEndCount(v);
		}
		else if (this.stack.size() == 3 && localName.equals(HASHCODE)) {
			BigInteger v = new BigInteger(this.currentText.toString(), 16);
			this.currentItem.setHashCode(v);
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
