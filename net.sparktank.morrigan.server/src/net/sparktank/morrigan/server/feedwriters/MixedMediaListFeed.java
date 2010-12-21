package net.sparktank.morrigan.server.feedwriters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.media.IMixedMediaList;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;

@Deprecated
public class MixedMediaListFeed<T extends IMixedMediaList<? extends IMixedMediaItem>> extends AbstractFeed {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final T ml;

	public MixedMediaListFeed (T ml) {
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
		
		for (IMixedMediaItem mi : this.ml.getMediaItems()) {
			dw.startElement("entry");
			
			String file;
			try {
				file = URLEncoder.encode(mi.getFilepath(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			
			addElement(dw, "title", mi.getTitle());
			addLink(dw, "/media/" + this.ml.getType() + "/" + filenameFromPath(this.ml.getListId()) + "/" + file, "self", "text/xml");
			if (mi.getDateAdded() != null) {
				addElement(dw, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
			}
			if (mi.getDateLastModified() != null) {
				addElement(dw, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
			}
			addElement(dw, "type", mi.getMediaType().getN());
			addElement(dw, "hash", mi.getHashcode());
			
			if (mi.getMediaType() == MediaType.TRACK) {
				if (mi.isPlayable()) {
					addLink(dw, "/player/0/play/" + listFile + "/" + file, "play", "cmd"); // FIXME list all players here.
				}
    			addElement(dw, "duration", mi.getDuration());
    			addElement(dw, "startcount", mi.getStartCount());
    			addElement(dw, "endcount", mi.getEndCount());
    			if (mi.getDateLastPlayed() != null) {
    				addElement(dw, "datelastplayed", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastPlayed()));
    			}
			}
			else if (mi.getMediaType() == MediaType.PICTURE) {
				addElement(dw, "width", mi.getWidth());
				addElement(dw, "height", mi.getHeight());
			}
			
			dw.endElement("entry");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
