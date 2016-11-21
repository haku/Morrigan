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

package com.vaguehope.morrigan.android.modelimpl;

import java.io.IOException;
import java.io.InputStream;
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
import com.vaguehope.morrigan.android.model.MlistState;
import com.vaguehope.morrigan.android.model.MlistStateList;
import com.vaguehope.morrigan.android.model.ServerReference;

public class MlistStateListImpl implements MlistStateList, ContentHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final List<MlistState> mlistStateList = new LinkedList<MlistState>();
	private final ServerReference serverReference;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MlistStateListImpl (final InputStream dataIs, final ServerReference serverReference) throws SAXException {
		this.serverReference = serverReference;

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
	public List<? extends MlistState> getMlistStateList() {
		return Collections.unmodifiableList(this.mlistStateList);
	}

	@Override
	public List<? extends Artifact> getArtifactList() {
		return Collections.unmodifiableList(this.mlistStateList);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Stack<String> stack = new Stack<String>();
	private StringBuilder currentText;
	private MlistStateBasicImpl currentItem;

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		this.stack.push(localName);

		if (this.stack.size() == 2 && localName.equals("entry")) {
			this.currentItem = new MlistStateBasicImpl();
		}
		else if (this.stack.size() == 3 && localName.equals("link")) {
			String relVal = attributes.getValue("rel");
			if (relVal != null && relVal.equals("self")) {
				String hrefVal = attributes.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					this.currentItem.setRelativePath(hrefVal);
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
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		if (this.stack.size() == 2 && localName.equals("entry")) {
			this.mlistStateList.add(this.currentItem);
			this.currentItem = null;
		}
		else if (this.stack.size() == 3 && localName.equals("title")) {
			this.currentItem.setTitle(this.currentText == null ? null : this.currentText.toString());
		}

		this.stack.pop();
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
        this.currentText.append( ch, start, length );
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void endDocument() throws SAXException { /* UNUSED */ }
	@Override
	public void endPrefixMapping(final String prefix) throws SAXException { /* UNUSED */ }
	@Override
	public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException { /* UNUSED */ }
	@Override
	public void processingInstruction(final String target, final String data) throws SAXException { /* UNUSED */ }
	@Override
	public void setDocumentLocator(final Locator locator) { /* UNUSED */ }
	@Override
	public void skippedEntity(final String name) throws SAXException { /* UNUSED */ }
	@Override
	public void startDocument() throws SAXException { /* UNUSED */ }
	@Override
	public void startPrefixMapping(final String prefix, final String uri) throws SAXException { /* UNUSED */ }

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getSortKey() {
		return "2";
	}

	@Override
	public int compareTo(final ArtifactList another) {
		return this.getSortKey().compareTo(another.getSortKey());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
