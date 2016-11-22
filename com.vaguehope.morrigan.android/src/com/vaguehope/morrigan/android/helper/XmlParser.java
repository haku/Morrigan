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

package com.vaguehope.morrigan.android.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.vaguehope.morrigan.android.model.ServerReference;

public class XmlParser implements ContentHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String XMLSTART = "<?xml";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final ServerReference serverReference;
	private final Map<String, String> nodes = new HashMap<String, String>();
	private final Map<String, List<String>> repeatingNodes = new HashMap<String, List<String>>();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public XmlParser (final String data, final String[] nodes, final ServerReference serverReference) throws SAXException {
		this.serverReference = serverReference;

		for (String node : nodes) {
			this.nodes.put(node, null);
		}

		String xml;
		if (data.startsWith(XMLSTART)) {
			xml = data;
		}
		else if (data.contains(XMLSTART)) {
			xml = data.substring(data.indexOf(XMLSTART));
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
				xmlReader.parse(new InputSource(new java.io.StringReader(xml)));
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

	public boolean hasNode (final String node) {
		return this.nodes.keySet().contains(node);
	}

	public String getNode (final String node) {
		return this.nodes.get(node);
	}

	public int getNodeInt (final String node) {
		String s = this.nodes.get(node);
		if (s == null) return -1;
		int i = Integer.parseInt(s);
		return i;
	}

	public long getNodeLong (final String node) {
		String s = this.nodes.get(node);
		if (s == null) return -1;
		long l = Long.parseLong(s);
		return l;
	}

	public boolean getNodeBoolean (final String node) {
		String s = this.nodes.get(node);
		boolean b = Boolean.parseBoolean(s);
		return b;
	}

	public List<String> getRepeatingNode (final String node) {
		return this.repeatingNodes.get(node);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Stack<String> stack = new Stack<String>();
	private StringBuilder currentText;

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		this.stack.push(localName);

		if (localName.equals("link")) {
			String relVal = attributes.getValue("rel");
			if (relVal != null) {
				String hrefVal = attributes.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					this.nodes.put(relVal, hrefVal);
				}
			}
		}

		// If we need a new StringBuilder, make one.
		if (this.currentText == null || this.currentText.length() > 0) {
			this.currentText = new StringBuilder();
		}
	}

	@Override
	public void endElement (final String uri, final String localName, final String qName) throws SAXException {
		final String newText = this.currentText.toString();
		final String prev = this.nodes.put(localName, newText);

		if (prev != null) {
			List<String> list = this.repeatingNodes.get(localName);
			if (list == null) {
				list = new ArrayList<String>();
				this.repeatingNodes.put(localName, list);
				list.add(prev);
			}
			list.add(newText);
		}

		this.stack.pop();
	}

	@Override
	public void characters (final char[] ch, final int start, final int length) throws SAXException {
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
}
