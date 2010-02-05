package net.sparktank.morrigan.model.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
	
	/**
	 * A unique identifier.
	 */
	public String getListId () {
		return listId;
	}
	
	/**
	 * A human readable name for the GUI.
	 * @return
	 */
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
	
	abstract public boolean allowDuplicateEntries ();
	
	/**
	 * This is the signal to read any source data needed.
	 * This will be called soon after the constructor and before
	 * any content is read.
	 * It may be called when no work needs doing and its
	 * up to the implemented to track this.
	 */
	abstract public void read () throws MorriganException;
	
	public int getCount () {
		return mediaTracks.size();
	}
	
	/**
	 * Returns an unmodifiable list of the playlist items.
	 * @return
	 */
	public List<MediaTrack> getMediaTracks() {
		return Collections.unmodifiableList(mediaTracks);
	}
	
	protected void replaceList (List<MediaTrack> mediaTracks) {
		this.mediaTracks = mediaTracks;
		setDirty(true);
	}
	
	public void addTrack (MediaTrack track) {
		if (allowDuplicateEntries() || !mediaTracks.contains(track)) {
			mediaTracks.add(track);
			setDirty(true);
		}
	}
	
	public void removeMediaTrack (MediaTrack track) {
		mediaTracks.remove(track);
		setDirty(true);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void incTrackStartCnt (MediaTrack track) throws MorriganException {
		track.setStartCount(track.getStartCount()+1);
		track.setDateLastPlayed(new Date());
		setDirty(true);
	}
	
	public void incTrackEndCnt (MediaTrack track) throws MorriganException {
		track.setEndCount(track.getEndCount()+1);
		setDirty(true);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString () {
		return listName + " ("+mediaTracks.size()+" items)";
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
