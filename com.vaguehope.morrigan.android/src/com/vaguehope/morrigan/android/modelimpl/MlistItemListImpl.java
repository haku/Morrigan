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
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.MlistItemList;

public class MlistItemListImpl implements MlistItemList, ContentHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String ENTRY = "entry";

	private static final String LINK = "link";
	private static final String REL = "rel";
	private static final String SELF = "self";
	private static final String HREF = "href";

	public static final String TITLE = "title";
	public static final String TYPE = "type";
	public static final String DURATION = "duration";
	public static final String STARTCOUNT = "startcount";
	public static final String ENDCOUNT = "endcount";
	public static final String FILESIZE = "filesize";
	public static final String HASHCODE = "hash";
	public static final String ENABLED = "enabled";
	public static final String MISSING = "missing";
	public static final String TAG = "tag";

	private static final String MANUAL_TAG = "0";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final List<MlistItem> mlistItemList = new LinkedList<MlistItem>();

	private final String query;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MlistItemListImpl (final InputStream dataIs, final String query) throws SAXException {
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

	@Override
	public void sort (final Comparator<MlistItem> comparator) {
		Collections.sort(this.mlistItemList, comparator);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final Map<String, String> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<String, String>());

	private final Stack<String> stack = new Stack<String>();
	private final Stack<Map<String, String>> currentAttributes = new Stack<Map<String,String>>();
	private StringBuilder currentText;
	private MlistItemBasicImpl currentItem;
	private List<String> currentTags;

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attr) throws SAXException {
		this.stack.push(localName);
		Map<String, String> atMap = EMPTY_MAP;

		if (this.stack.size() == 2 && localName.equals(ENTRY)) {
			this.currentItem = new MlistItemBasicImpl();
		}
		else if (this.stack.size() == 3 && localName.equals(LINK)) {
			String relVal = attr.getValue(REL);
			if (relVal != null && relVal.equals(SELF)) {
				String hrefVal = attr.getValue(HREF);
				if (hrefVal != null && hrefVal.length() > 0) {
					this.currentItem.setRelativeUrl(hrefVal);
				}
			}
		}
		else if (attr.getLength() > 0 && this.stack.size() == 3 && localName.equals(TAG)) {
			atMap = new HashMap<String, String>();
			for (int i = 0; i < attr.getLength(); i++) atMap.put(attr.getLocalName(i), attr.getValue(i));
		}

		// If we need a new StringBuilder, make one.
		if (this.currentText == null || this.currentText.length() > 0) {
			this.currentText = new StringBuilder();
		}
		this.currentAttributes.push(atMap);
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		Map<String, String> attr = this.currentAttributes.pop();

		if (this.stack.size() == 2 && localName.equals(ENTRY)) {
			if (this.currentTags != null) {
				this.currentItem.setTags(this.currentTags.toArray(new String[this.currentTags.size()]));
				this.currentTags = null;
			}
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
		else if (this.stack.size() == 3 && localName.equals(FILESIZE)) {
			long v = Long.parseLong(this.currentText.toString());
			this.currentItem.setFileSize(v);
		}
		else if (this.stack.size() == 3 && localName.equals(HASHCODE)) {
			BigInteger v = new BigInteger(this.currentText.toString(), 16);
			this.currentItem.setHashCode(v);
		}
		else if (this.stack.size() == 3 && localName.equals(ENABLED)) {
			boolean v = Boolean.parseBoolean(this.currentText.toString());
			this.currentItem.setEnabled(v);
		}
		else if (this.stack.size() == 3 && localName.equals(MISSING)) {
			boolean v = Boolean.parseBoolean(this.currentText.toString());
			this.currentItem.setMissing(v);
		}
		else if (this.stack.size() == 3 && localName.equals(TAG)) {
			if (MANUAL_TAG.equals(attr.get("t")) && "".equals(attr.get("c"))) {
				if (this.currentTags == null) this.currentTags = new LinkedList<String>();
				this.currentTags.add(this.currentText.toString());
			}
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
		return ""; // This should never be relevant.
	}

	@Override
	public int compareTo(final ArtifactList another) {
		return this.getSortKey().compareTo(another.getSortKey());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
