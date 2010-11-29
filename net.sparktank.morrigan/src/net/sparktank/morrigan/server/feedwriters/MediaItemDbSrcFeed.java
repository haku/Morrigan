package net.sparktank.morrigan.server.feedwriters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.impl.MediaItemDb;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;

public class MediaItemDbSrcFeed extends AbstractFeed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private MediaItemDb<?,?,?> ml;
	
	public MediaItemDbSrcFeed (MediaItemDb<?,?,?> ml) {
		super();
		if (ml==null) throw new IllegalArgumentException("MediaList paramater can not be null.");
		this.ml = ml;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void populateFeed(DataWriter dw) throws SAXException, MorriganException {
		this.ml.read();
		
		String listFile;
		try {
			listFile = URLEncoder.encode(filenameFromPath(this.ml.getListId()), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		addElement(dw, "title", this.ml.getListName() + " src");
		addLink(dw, "/media/" + this.ml.getType() + "/" + listFile + "/src", "self", "text/xml");
		addLink(dw, "/media/" + this.ml.getType() + "/" + listFile, "library", "text/xml");
		addLink(dw, "/media/" + this.ml.getType() + "/" + listFile + "/src/add", "add", "cmd");
		addLink(dw, "/media/" + this.ml.getType() + "/" + listFile + "/src/remove", "remove", "cmd");
		
		List<String> src;
		src = this.ml.getSources();
		
		for (String s : src) {
			dw.startElement("entry");
			
			addElement(dw, "dir", s);

			dw.endElement("entry");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
