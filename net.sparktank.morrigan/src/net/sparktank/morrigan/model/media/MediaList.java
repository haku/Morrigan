package net.sparktank.morrigan.model.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String listName = null;
	private List<MediaTrack> mediaTracks = new ArrayList<MediaTrack>();
	
	public MediaList (String listName) {
		this.listName = listName;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getListName () {
		return listName;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private boolean isDirty = false;
	private ArrayList<Runnable> dirtyChangeEvents = new ArrayList<Runnable>();
	private ArrayList<Runnable> changeEvents = new ArrayList<Runnable>();
	
	public void setDirty (boolean dirty) {
		boolean change = (isDirty != dirty);
		
		isDirty = dirty;
		
		if (change) {
			for (Runnable r : dirtyChangeEvents) {
				r.run();
			}
		}
		
		for (Runnable r : changeEvents) {
			r.run();
		}
	}
	
	public boolean isDirty () {
		return isDirty;
	}
	
	public void addDirtyChangeEvent (Runnable r) {
		dirtyChangeEvents.add(r);
	}
	
	public void removeDirtyChangeEvent (Runnable r) {
		dirtyChangeEvents.remove(r);
	}
	
	public void addChangeEvent (Runnable r) {
		changeEvents.add(r);
	}
	
	public void removeChangeEvent (Runnable r) {
		changeEvents.remove(r);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
