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

package com.vaguehope.morrigan.android.model.impl;

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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.vaguehope.morrigan.android.model.Artifact;
import com.vaguehope.morrigan.android.model.ArtifactList;
import com.vaguehope.morrigan.android.model.PlayerQueue;
import com.vaguehope.morrigan.android.model.PlayerReference;

public class PlayerQueueImpl implements PlayerQueue, ContentHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String ENTRY = "entry";
	private static final String TITLE = "title";
	private static final String LISTREL = "list";
	private static final String ITEMREL = "item";
	private static final String ID = "id";
	private static final String HASH = "hash";
	private static final String DURATION = "duration";
	private static final String STARTCOUNT = "startcount";
	private static final String ENDCOUNT = "endcount";
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Stack<String> stack = new Stack<String>();
	private StringBuilder currentText;
	
	private String currentTitle = null;
	private String currentListRelativeUrl = null;
	private String currentItemRelativeUrl = null;
	private int currentId;
	private BigInteger currentHash = null;
	private int currentDuration;
	private int currentStartCount;
	private int currentEndCount;
	
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
			if (relVal != null && relVal.equals(ITEMREL)) {
				String hrefVal = attributes.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					this.currentItemRelativeUrl = hrefVal;
				}
			}
			else if (relVal != null && relVal.equals(LISTREL)) {
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
				item.setTrackTitle(this.currentTitle);
				item.setRelativeUrl(this.currentItemRelativeUrl);
				item.setType(1); // TODO reference an enum?
				item.setId(this.currentId);
				item.setHashCode(this.currentHash);
				item.setDuration(this.currentDuration);
				item.setStartCount(this.currentStartCount);
				item.setEndCount(this.currentEndCount);
				this.artifactList.add(item);
			}
			else {
				MlistStateBasicImpl list = new MlistStateBasicImpl();
				list.setTitle(this.currentTitle);
				list.setBaseUrl(this.playerReference.getServerReference().getBaseUrl() + this.currentListRelativeUrl);
				list.setId(this.currentId);
				
				this.artifactList.add(list);
			}
		}
		else if (this.stack.size() == 3 && localName.equals(TITLE)) {
			this.currentTitle = this.currentText.toString();
		}
		else if (this.stack.size() == 3 && localName.equals(ID)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentId = v;
		}
		else if (this.stack.size() == 3 && localName.equals(HASH)) {
			BigInteger v = new BigInteger(this.currentText.toString(), 16);
			this.currentHash = v;
		}
		else if (this.stack.size() == 3 && localName.equals(DURATION)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentDuration = v;
		}
		else if (this.stack.size() == 3 && localName.equals(STARTCOUNT)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentStartCount = v;
		}
		else if (this.stack.size() == 3 && localName.equals(ENDCOUNT)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentEndCount = v;
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
