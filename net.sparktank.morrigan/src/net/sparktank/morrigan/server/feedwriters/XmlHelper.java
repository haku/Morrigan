package net.sparktank.morrigan.server.feedwriters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

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
		Document document;
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(xmlString.getBytes());
			document = getDocumentBuilder().parse(is);
		} catch (SAXException e) {
			System.err.println("SAXException parsing xml:\n" + xmlString);
			throw e;
		}
		return document;
	}
	
	static private ThreadLocal<SimpleDateFormat> Iso8601Utc = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			SimpleDateFormat a = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			a.setTimeZone(TimeZone.getTimeZone("UTC"));
			return a;
		}
	};
	
	static public DateFormat getIso8601UtcDateFormatter () {
		return Iso8601Utc.get();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
