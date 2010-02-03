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
	
	private boolean isDirty = false;
	private Runnable dirtyChangeEvent = null;
	
	public void setDirty (boolean dirty) {
		boolean change = false;
		if (dirtyChangeEvent!=null) {
			if (isDirty != dirty) {
				change = true;
			}
		}
		isDirty = dirty;
		if (change) dirtyChangeEvent.run();
	}
	
	public boolean isDirty () {
		return isDirty;
	}
	
	public void setDirtyChangeEvent (Runnable r) {
		this.dirtyChangeEvent = r;
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
		setDirty(true);
		return mt;
	}
	
	public void removeMediaTrack (MediaTrack track) {
		mediaTracks.remove(track);
		setDirty(true);
	}
	
	@Override
	public String toString () {
		return listName + " ("+mediaTracks.size()+" items)";
	}
	
}
