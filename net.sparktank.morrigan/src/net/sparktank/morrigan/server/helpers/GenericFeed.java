package net.sparktank.morrigan.server.helpers;

import java.io.File;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
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
		
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(getDoc());
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
		
		return result.getWriter().toString();
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
	
	static public void addLink (Document doc, Node element, String href) {
		addLink(doc, element, href, null);
	}
	
	static public void addLink (Document doc, Node element, String href, String rel) {
		Element link = doc.createElement("link");
		
		if (rel != null) {
			Attr attRel = doc.createAttribute("rel");
			attRel.setValue(rel);
			link.getAttributes().setNamedItem(attRel);
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
