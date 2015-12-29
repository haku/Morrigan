package com.vaguehope.morrigan.server.util;

import java.io.File;
import java.io.PrintWriter;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.megginson.sax.DataWriter;

public final class FeedHelper {

	private FeedHelper () {
		throw new AssertionError();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String CDATA = "CDATA";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String FEED = "feed";
	private static final String FEEDNS = "http://www.w3.org/2005/Atom";

	public static DataWriter startFeed (final PrintWriter out) throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "xmlns", "", CDATA, FEEDNS);
		DataWriter dw = startDocument(out, FEED, atts);

		return dw;
	}

	public static void endFeed (final DataWriter dw) throws SAXException {
		endDocument(dw, FEED);
	}

//	-  -  -  -  -  -  -  -  -  -  -  -

	public static DataWriter startDocument (final PrintWriter out) throws SAXException {
		return startDocument(out, null, null);
	}

	public static DataWriter startDocument (final PrintWriter out, final String mainElement) throws SAXException {
		return startDocument(out, mainElement, null);
	}

	public static DataWriter startDocument (final PrintWriter out, final String mainElement, final AttributesImpl atts) throws SAXException {
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

	public static void endDocument (final DataWriter dw) throws SAXException {
		endDocument(dw, null);
	}

	public static void endDocument (final DataWriter dw, final String mainElement) throws SAXException {
		if (mainElement != null) dw.endElement(mainElement);
		dw.endDocument();
	}

//	-  -  -  -  -  -  -  -  -  -  -  -

	public static void startElement (final DataWriter dw, final String newElement, final String[][] attrs) throws SAXException {
		final AttributesImpl atts = new AttributesImpl();
		for (String[] attr : attrs) {
			String key = attr[0];
			String value = attr[1];
			atts.addAttribute("", key, "", CDATA, value);
		}
		dw.startElement("", newElement, "", atts);
	}

//	-  -  -  -  -  -  -  -  -  -  -  -

	public static void addElement (final DataWriter dw, final String newElement, final int i) throws SAXException {
		addElement(dw, newElement, String.valueOf(i));
	}

	public static void addElement (final DataWriter dw, final String newElement, final long l) throws SAXException {
		addElement(dw, newElement, String.valueOf(l));
	}

	public static void addElement (final DataWriter dw, final String newElement, final String textContent) throws SAXException {
		dw.dataElement(newElement, textContent);
	}

	public static void addElement (final DataWriter dw, final String newElement, final String textContent, final String[][] attrs) throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		for (String[] attr : attrs) {
			String key = attr[0];
			String value = attr[1];
			atts.addAttribute("", key, "", CDATA, value);
		}

		dw.dataElement("", newElement, null, atts, textContent);
	}

	public static void addLink (final DataWriter dw, final String href, final String rel) throws SAXException {
		addLink(dw, href, rel, null);
	}

	public static void addLink (final DataWriter dw, final String href, final String rel, final String type) throws SAXException {
		AttributesImpl atts = new AttributesImpl();

		if (rel != null) {
			atts.addAttribute("", "rel", "", CDATA, rel);
		}

		if (type != null) {
			atts.addAttribute("", "type", "", CDATA, type);
		}

		atts.addAttribute("", "href", "", CDATA, href);

		dw.emptyElement("", "link", "", atts);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static String filenameFromPath (final String path) {
		return path.substring(path.lastIndexOf(File.separator) + 1);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
