package net.sparktank.morrigan.model.media;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MediaList {
	
	private String listName = null;
	private List<MediaTrack> mediaTracks = new ArrayList<MediaTrack>();
	
	public MediaList (String listName) {
		this.listName = listName;
	}
	
	public String getListName () {
		return listName;
	}
	
	public List<MediaTrack> getMediaTracks() {
		return mediaTracks;
	}
	
	public MediaTrack addTrack (String mediaFilePath) {
		MediaTrack mt = new MediaTrack(mediaFilePath);
		mediaTracks.add(mt);
		return mt;
	}
	
	@Override
	public String toString () {
		StringBuilder ret = new StringBuilder();
		ret.append(listName + ":");
		
		Iterator<MediaTrack> itr = mediaTracks.iterator();
	    while (itr.hasNext()) {
	    	MediaTrack item = itr.next();
	    	ret.append("\n");
	    	ret.append(item.getFilepath());
	    }
		
		return ret.toString();
	}
	
}
