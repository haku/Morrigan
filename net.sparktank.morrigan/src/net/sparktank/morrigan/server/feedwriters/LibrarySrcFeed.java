package net.sparktank.morrigan.server.feedwriters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.library.local.LocalMediaLibrary;
import net.sparktank.sqlitewrapper.DbException;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;

public class LibrarySrcFeed extends Feed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private LocalMediaLibrary ml;
	
	public LibrarySrcFeed (LocalMediaLibrary ml) throws MorriganException {
		super();
		if (ml==null) throw new IllegalArgumentException("MediaList paramater can not be null.");
		this.ml = ml;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected void populateFeed(DataWriter dw) throws SAXException, MorriganException {
		ml.read();
		
		String listFile;
		try {
			listFile = URLEncoder.encode(filenameFromPath(ml.getListId()), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		addElement(dw, "title", ml.getListName() + " src");
		addLink(dw, "/media/" + ml.getType() + "/" + listFile + "/src", "self", "text/xml");
		addLink(dw, "/media/" + ml.getType() + "/" + listFile, "library", "text/xml");
		addLink(dw, "/media/" + ml.getType() + "/" + listFile + "/src/add", "add", "cmd");
		addLink(dw, "/media/" + ml.getType() + "/" + listFile + "/src/remove", "remove", "cmd");
		
		List<String> src;
		try {
			src = ml.getSources();
		} catch (DbException e) {
			throw new MorriganException(e);
		}
		
		for (String s : src) {
			dw.startElement("entry");
			
			addElement(dw, "dir", s);

			dw.endElement("entry");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
