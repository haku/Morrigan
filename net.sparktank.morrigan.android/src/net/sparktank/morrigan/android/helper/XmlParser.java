package net.sparktank.morrigan.android.helper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XmlParser implements ContentHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String XMLSTART = "<?xml";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Map<String, String> nodes = new HashMap<String, String>();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public XmlParser (String data, String[] nodes) throws SAXException {
		
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
	
	public boolean hasNode (String node) {
		return this.nodes.keySet().contains(node);
	}
	
	public String getNode (String node) {
		return this.nodes.get(node);
	}
	
	public int getNodeInt (String node) {
		String s = this.nodes.get(node);
		int i = Integer.parseInt(s);
		return i;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String inNode = null;
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		this.inNode = localName.trim();
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		this.inNode = null;
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.inNode!=null) {
        	String chars = (new String(ch).substring(start, start + length));
        	
        	String prev=this.nodes.get(this.inNode);
        	if (prev==null) {
        		this.nodes.put(this.inNode, chars);
        	} else {
        		this.nodes.put(this.inNode, prev.concat(chars));
        	}
        }
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
