package net.sparktank.morrigan.model.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;

public abstract class MediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String listId;
	private final String listName;
	private List<MediaTrack> mediaTracks = new ArrayList<MediaTrack>();
	
	/**
	 * listId must be unique.  It will be used to identify
	 * the matching editor.
	 * @param listId a unique ID.
	 * @param listName a human-readable title for this list.
	 */
	protected MediaList (String listId, String listName) {
		this.listId = listId;
		this.listName = listName;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getListId () {
		return listId;
	}
	
	public String getListName () {
		return listName;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private boolean canBeDirty = true;
	private boolean isDirty = false;
	private ArrayList<Runnable> dirtyChangeEvents = new ArrayList<Runnable>();
	private ArrayList<Runnable> changeEvents = new ArrayList<Runnable>();
	
	protected boolean isCanBeDirty () {
		return canBeDirty;
	}
	
	protected void setCanBeDirty (boolean value) {
		canBeDirty = value;
	}
	
	public void setDirty (boolean dirty) {
		if (canBeDirty) {
			boolean change = (isDirty != dirty);
			
			isDirty = dirty;
			
			if (change) {
				for (Runnable r : dirtyChangeEvents) {
					r.run();
				}
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
	 * This is the signal to read any source data needed.
	 * This will be called soon after the constructor and before
	 * any content is read.
	 */
	abstract public void read () throws MorriganException;
	
	/**
	 * Returns an unmodifiable list of the playlist items.
	 * @return
	 */
	public List<MediaTrack> getMediaTracks() {
		return Collections.unmodifiableList(mediaTracks);
	}
	
	public void addTrack (MediaTrack track) {
		mediaTracks.add(track);
		setDirty(true);
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
