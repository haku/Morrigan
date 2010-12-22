package net.sparktank.morrigan.server.feedwriters;

import java.io.File;
import java.io.PrintWriter;

import net.sparktank.morrigan.model.exceptions.MorriganException;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.megginson.sax.DataWriter;

// TODO make into static helper class.
public abstract class AbstractFeed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void process (PrintWriter out) throws SAXException, MorriganException {
		DataWriter dw = startFeed(out);
		populateFeed(dw);
		endFeed(dw);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected abstract void populateFeed (DataWriter dw) throws SAXException, MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String FEED = "feed";
	private static final String FEEDNS = "http://www.w3.org/2005/Atom";
	
	static public DataWriter startFeed (PrintWriter out) throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "xmlns", "", "CDATA", FEEDNS);
		DataWriter dw = startDocument(out, FEED, atts);
		
		return dw;
	}
	
	static public void endFeed (DataWriter dw) throws SAXException {
		endDocument(dw, FEED);
	}
	
//	-  -  -  -  -  -  -  -  -  -  -  -
	
	static public DataWriter startDocument (PrintWriter out) throws SAXException {
		return startDocument(out, null, null);
	}
	
	static public DataWriter startDocument (PrintWriter out, String mainElement) throws SAXException {
		return startDocument(out, mainElement, null);
	}
	
	static public DataWriter startDocument (PrintWriter out, String mainElement, AttributesImpl atts) throws SAXException {
		DataWriter dw = new DataWriter(out);
		
		// TODO set UTF-8 in header.
		dw.startDocument();
		dw.setIndentStep(1);
		
		if (mainElement != null) {
			if (atts != null) {
				dw.startElement("", mainElement, "", atts);
			}
			else {
				dw.startElement("", mainElement);
			}
		}
		
		return dw;
	}
	
	static public void endDocument (DataWriter dw) throws SAXException {
		endDocument(dw, null);
	}
	
	static public void endDocument (DataWriter dw, String mainElement) throws SAXException {
		if (mainElement != null) dw.endElement(mainElement);
		dw.endDocument();
	}
	
//	-  -  -  -  -  -  -  -  -  -  -  -
	
	static public void addElement (DataWriter dw, String newElement, int i) throws SAXException {
		addElement(dw, newElement, String.valueOf(i));
	}
	
	static public void addElement (DataWriter dw, String newElement, long l) throws SAXException {
		addElement(dw, newElement, String.valueOf(l));
	}
	
	static public void addElement (DataWriter dw, String newElement, String textContent) throws SAXException {
		dw.dataElement(newElement, textContent);
	}
	
	static public void addLink (DataWriter dw, String href, String rel) throws SAXException {
		addLink(dw, href, rel, null);
	}
	
	static public void addLink (DataWriter dw, String href, String rel, String type) throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		
		if (rel != null) {
			atts.addAttribute("", "rel", "", "CDATA", rel);
		}
		
		if (type != null) {
			atts.addAttribute("", "type", "", "CDATA", type);
		}
		
		atts.addAttribute("", "href", "", "CDATA", href);
		
		dw.emptyElement("", "link", "", atts);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public String filenameFromPath (String path) {
		return path.substring(path.lastIndexOf(File.separator) + 1);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
