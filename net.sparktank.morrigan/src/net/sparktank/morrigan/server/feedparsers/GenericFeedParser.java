package net.sparktank.morrigan.server.feedparsers;

import java.io.IOException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import net.sparktank.morrigan.server.feedwriters.XmlHelper;

public class GenericFeedParser {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Document doc;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GenericFeedParser (String xmlString) throws SAXException, IOException {
		doc = XmlHelper.xmlStringToDocument(xmlString);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected Document getDoc () {
		return doc;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
