package net.sparktank.morrigan.model.media;

import java.util.ArrayList;
import java.util.Collections;
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
	
	/**
	 * Returns an unmodifiable list of the playlist items.
	 * @return
	 */
	public List<MediaTrack> getMediaTracks() {
		return Collections.unmodifiableList(mediaTracks);
	}
	
	public MediaTrack addTrack (String mediaFilePath) {
		MediaTrack mt = new MediaTrack(mediaFilePath);
		mediaTracks.add(mt);
		return mt;
	}
	
	public void removeMediaTrack (MediaTrack track) {
		mediaTracks.remove(track);
	}
	
	@Override
	public String toString () {
		return listName + " ("+mediaTracks.size()+" items)";
	}
	
}
