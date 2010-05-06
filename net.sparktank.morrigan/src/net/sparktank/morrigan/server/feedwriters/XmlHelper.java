package net.sparktank.morrigan.server.feedwriters;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private DocumentBuilder documentBuilder = null;
	
	static public DocumentBuilder getDocumentBuilder () {
		if (documentBuilder == null) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setValidating(false);
			
			try {
				documentBuilder = factory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			}
			documentBuilder.setErrorHandler(null);
		}
		
		return documentBuilder;
	}
	
	static public Document xmlStringToDocument (String xmlString) throws SAXException, IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(xmlString.getBytes());
		return getDocumentBuilder().parse(is);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
