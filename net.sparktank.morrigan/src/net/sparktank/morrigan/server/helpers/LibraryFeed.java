package net.sparktank.morrigan.server.helpers;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.library.MediaLibrary;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class LibraryFeed extends GenericFeed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public LibraryFeed (MediaLibrary ml) throws MorriganException {
		super();
		mediaLibraryToFeed(ml, getDoc());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void mediaLibraryToFeed (MediaLibrary ml, Document doc) throws MorriganException {
		ml.read();
		
		Node firstChild = doc.getFirstChild(); // This should get the "feed" element.
		
		for (MediaItem mi : ml.getMediaTracks()) {
			Element entry = doc.createElement("entry");
			try {
				String file = URLEncoder.encode(filenameFromPath(mi.getFilepath()),"UTF-8");
				
				addElement(doc, entry, "title", mi.getTitle());
				addLink(doc, entry, "/media/library/" + filenameFromPath(ml.getListId()) + "/" + file);
				addLink(doc, entry, "/player/0/play/" + file, "play"); // FIXME list all players here.
				addElement(doc, entry, "duration", mi.getDuration());
				addElement(doc, entry, "hash", mi.getHashcode());
				addElement(doc, entry, "startcount", mi.getStartCount());
				addElement(doc, entry, "endcount", mi.getEndCount());
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			firstChild.appendChild(entry);
		}
	}
	
	static private void addElement (Document doc,Element element, String newElement, int i) {
		addElement(doc, element, newElement, String.valueOf(i));
	}
	
	static private void addElement (Document doc,Element element, String newElement, long l) {
		addElement(doc, element, newElement, String.valueOf(l));
	}
	
	static private void addElement (Document doc,Element element, String newElement, String textContent) {
		Element e = doc.createElement(newElement);
		e.setTextContent(textContent);
		element.appendChild(e);
	}
	
	static private void addLink (Document doc, Element element, String href) {
		addLink(doc, element, href, null);
	}
	
	static private void addLink (Document doc, Element element, String href, String rel) {
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
	
	static private String filenameFromPath (String path) {
		return path.substring(path.lastIndexOf(File.separator) + 1);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
