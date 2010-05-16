package net.sparktank.morrigan.server.feedwriters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.library.LocalMediaLibrary;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class LibrarySrcFeed extends GenericFeed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public LibrarySrcFeed (LocalMediaLibrary ml) throws MorriganException {
		super();
		mediaLibrarySrcToFeed(ml, getDoc());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void mediaLibrarySrcToFeed(LocalMediaLibrary ml, Document doc) throws MorriganException {
		ml.read();
		
		String listFile;
		try {
			listFile = URLEncoder.encode(filenameFromPath(ml.getListId()), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		Node feed = doc.getFirstChild(); // This should get the "feed" element.
		addElement(doc, feed, "title", ml.getListName() + " src");
		addLink(doc, feed, "/media/" + ml.getType() + "/" + listFile + "/src", "self", "text/xml");
		addLink(doc, feed, "/media/" + ml.getType() + "/" + listFile, "library", "text/xml");
		addLink(doc, feed, "/media/" + ml.getType() + "/" + listFile + "/src/add", "add", "cmd");
		addLink(doc, feed, "/media/" + ml.getType() + "/" + listFile + "/src/remove", "remove", "cmd");
		
		List<String> src = ml.getSources();
		for (String s : src) {
			Element entry = doc.createElement("entry");
			
			addElement(doc, entry, "dir", s);
			
			feed.appendChild(entry);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
