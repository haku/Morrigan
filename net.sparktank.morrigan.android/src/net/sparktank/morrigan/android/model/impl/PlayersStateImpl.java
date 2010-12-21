/*
 * Copyright 2010 Fae Hutter
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
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sparktank.morrigan.android.helper.XmlParser;
import net.sparktank.morrigan.android.model.Artifact;
import net.sparktank.morrigan.android.model.PlayState;
import net.sparktank.morrigan.android.model.PlayerState;
import net.sparktank.morrigan.android.model.PlayersState;
import net.sparktank.morrigan.android.model.ServerReference;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class PlayersStateImpl implements PlayersState, ContentHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public final List<PlayerState> playersState = new LinkedList<PlayerState>();
	private final ServerReference serverReference;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PlayersStateImpl (String data, ServerReference serverReference) throws SAXException {
		this.serverReference = serverReference;
		String xml;
		if (data.startsWith(XmlParser.XMLSTART)) {
			xml = data;
		}
		else if (data.contains(XmlParser.XMLSTART)) {
			xml = data.substring(data.indexOf(XmlParser.XMLSTART));
		}
		else {
			throw new SAXException("Data does not contain XML.");
		}
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp;
		try {
			sp = spf.newSAXParser();
			XMLReader xmlReader = sp.getXMLReader();
			xmlReader.setContentHandler(this);
			try {
				xmlReader.parse(new InputSource(new StringReader(xml)));
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
	public List<PlayerState> getPlayersState() {
		return Collections.unmodifiableList(this.playersState);
	}
	
	@Override
	public List<? extends Artifact> getArtifacts() {
		return Collections.unmodifiableList(this.playersState);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Stack<String> stack = new Stack<String>();
	private StringBuilder currentText;
	private PlayerStateBasicImpl currentItem;
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		this.stack.push(localName);
		
		if (this.stack.size() == 2 && localName.equals("entry")) {
			this.currentItem = new PlayerStateBasicImpl();
		}
		else if (this.stack.size() == 3 && localName.equals("link")) {
			String relVal = attributes.getValue("rel");
			if (relVal != null && relVal.equals("self")) {
				String hrefVal = attributes.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					// Log.d("Morrigan", "hrefVal=" + hrefVal); // e.g. '/players/0'.
					this.currentItem.setBaseUrl(this.serverReference.getBaseUrl() + hrefVal);
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
		if (this.stack.size() == 2 && localName.equals("entry")) {
			this.playersState.add(this.currentItem);
			this.currentItem = null;
		}
		else if (this.stack.size() == 3 && localName.equals(PlayerStateXmlImpl.PLAYERID)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setId(v);
		}
		else if (this.stack.size() == 3 && localName.equals(PlayerStateXmlImpl.PLAYSTATE)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setPlayState(PlayState.parseN(v));
		}
		else if (this.stack.size() == 3 && localName.equals(PlayerStateXmlImpl.PLAYORDER)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setPlayOrder(v);
		}
		else if (this.stack.size() == 3 && localName.equals(PlayerStateXmlImpl.QUEUELENGTH)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setQueueLength(v);
		}
//		else if (this.stack.size() == 3 && localName.equals(PlayerStateParser.QUEUEDURATION)) {
//			TODO
//		}
		else if (this.stack.size() == 3 && localName.equals(PlayerStateXmlImpl.LISTTITLE)) {
			this.currentItem.setListTitle(this.currentText.toString());
		}
		else if (this.stack.size() == 3 && localName.equals(PlayerStateXmlImpl.LISTID)) {
			this.currentItem.setListId(this.currentText.toString());
		}
		else if (this.stack.size() == 3 && localName.equals(PlayerStateXmlImpl.TRACKTITLE)) {
			this.currentItem.setTrackTitle(this.currentText.toString());
		}
		else if (this.stack.size() == 3 && localName.equals(PlayerStateXmlImpl.PLAYPOSITION)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setPlayerPosition(v);
		}
		else if (this.stack.size() == 3 && localName.equals(PlayerStateXmlImpl.TRACKFILE)) {
			this.currentItem.setTrackFile(this.currentText.toString());
		}
		else if (this.stack.size() == 3 && localName.equals(PlayerStateXmlImpl.TRACKDURATION)) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setTrackDuration(v);
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
}
