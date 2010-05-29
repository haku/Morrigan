package net.sparktank.morrigan.server.feedwriters;

import java.io.File;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class GenericFeed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Document doc;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GenericFeed () {
		doc = XmlHelper.getDocumentBuilder().newDocument();
		Element e = doc.createElement("feed");
		e.setAttribute("xmlns", "http://www.w3.org/2005/Atom");
		doc.appendChild(e);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected Document getDoc () {
		return doc;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getXmlString () {
		Transformer transformer;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new RuntimeException(e);
		}
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
		
		StringWriter stringWriter = new StringWriter();
		Result streamResult = new StreamResult(stringWriter);
		DOMSource source = new DOMSource(getDoc());
		try {
			transformer.transform(source, streamResult);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
		
		return stringWriter.getBuffer().toString();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void addElement (Document doc, Node element, String newElement, int i) {
		addElement(doc, element, newElement, String.valueOf(i));
	}
	
	static public void addElement (Document doc, Node element, String newElement, long l) {
		addElement(doc, element, newElement, String.valueOf(l));
	}
	
	static public void addElement (Document doc, Node element, String newElement, String textContent) {
		Element e = doc.createElement(newElement);
		e.setTextContent(textContent);
		element.appendChild(e);
	}
	
	static public void addLink (Document doc, Node element, String href, String rel, String type) {
		Element link = doc.createElement("link");
		
		if (rel != null) {
			Attr attRel = doc.createAttribute("rel");
			attRel.setValue(rel);
			link.getAttributes().setNamedItem(attRel);
		}
		
		if (type != null) {
			Attr attType = doc.createAttribute("type");
			attType.setValue(type);
			link.getAttributes().setNamedItem(attType);
		}
		
		Attr attHref = doc.createAttribute("href");
		attHref.setValue(href);
		link.getAttributes().setNamedItem(attHref);
		
		element.appendChild(link);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public String filenameFromPath (String path) {
		return path.substring(path.lastIndexOf(File.separator) + 1);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
