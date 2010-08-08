package net.sparktank.morrigan.server.feedwriters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.tracks.IMediaTrackList;
import net.sparktank.morrigan.model.tracks.MediaTrack;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;

public class MediaListFeed<T extends IMediaTrackList<? extends MediaItem>> extends Feed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final T ml;

	public MediaListFeed (T ml) {
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
		
		dw.dataElement("title", this.ml.getListName());
		dw.dataElement("count", String.valueOf(this.ml.getCount()));
		
		addLink(dw, "/media/" + this.ml.getType() + "/" + listFile, "self", "text/xml");
		addLink(dw, "/media/" + this.ml.getType() + "/" + listFile + "/src", "src", "text/xml");
		addLink(dw, "/media/" + this.ml.getType() + "/" + listFile + "/scan", "scan", "cmd");
		addLink(dw, "/player/0/play/" + listFile, "play", "cmd"); // FIXME list all players here.
		
		for (MediaTrack mi : this.ml.getMediaTracks()) {
			dw.startElement("entry");
			
			String file;
			try {
				file = URLEncoder.encode(mi.getFilepath(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			
			addElement(dw, "title", mi.getTitle());
			addLink(dw, "/media/" + this.ml.getType() + "/" + filenameFromPath(this.ml.getListId()) + "/" + file, "self", "text/xml");
			addLink(dw, "/player/0/play/" + listFile + "/" + file, "play", "cmd"); // FIXME list all players here.
			addElement(dw, "duration", mi.getDuration());
			addElement(dw, "hash", mi.getHashcode());
			addElement(dw, "startcount", mi.getStartCount());
			addElement(dw, "endcount", mi.getEndCount());
			if (mi.getDateAdded() != null) {
				addElement(dw, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
			}
			if (mi.getDateLastModified() != null) {
				addElement(dw, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
			}
			if (mi.getDateLastPlayed() != null) {
				addElement(dw, "datelastplayed", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastPlayed()));
			}
			
			dw.endElement("entry");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
