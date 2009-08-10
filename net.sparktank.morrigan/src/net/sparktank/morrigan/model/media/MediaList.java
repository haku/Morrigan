package net.sparktank.morrigan.model.media;

import java.util.ArrayList;
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
	
}
