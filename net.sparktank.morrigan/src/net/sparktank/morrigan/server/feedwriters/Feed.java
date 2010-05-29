package net.sparktank.morrigan.server.feedwriters;

import java.io.File;
import java.io.PrintWriter;

import net.sparktank.morrigan.exceptions.MorriganException;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.megginson.sax.DataWriter;

public abstract class Feed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void process (PrintWriter out) throws SAXException, MorriganException {
		DataWriter dw = new DataWriter(out);
		
		dw.startDocument();
		dw.setIndentStep(1);
		
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "xmlns", "", "CDATA", "http://www.w3.org/2005/Atom");
		dw.startElement("", "feed", "", atts);
		
		populateFeed(dw);
		
		dw.endElement("feed");
		dw.endDocument();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected abstract void populateFeed (DataWriter dw) throws SAXException, MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void addElement (DataWriter dw, String newElement, int i) throws SAXException {
		addElement(dw, newElement, String.valueOf(i));
	}
	
	static public void addElement (DataWriter dw, String newElement, long l) throws SAXException {
		addElement(dw, newElement, String.valueOf(l));
	}
	
	static public void addElement (DataWriter dw, String newElement, String textContent) throws SAXException {
		dw.dataElement(newElement, textContent);
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
