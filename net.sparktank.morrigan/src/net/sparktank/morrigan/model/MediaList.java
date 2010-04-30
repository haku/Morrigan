package net.sparktank.morrigan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;

public abstract class MediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public enum DirtyState { CLEAN, DIRTY, METADATA };
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String listId;
	private final String listName;
	private List<MediaItem> mediaTracks = new ArrayList<MediaItem>();
	
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
	
	abstract public String getType ();
	
	abstract public String getSerial ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private DirtyState dirtyState = DirtyState.CLEAN;
	private ArrayList<Runnable> dirtyChangeEvents = new ArrayList<Runnable>();
	private ArrayList<Runnable> changeEvents = new ArrayList<Runnable>();
	
	abstract public boolean isCanBeDirty (); 
	
	protected void setDirtyState (DirtyState state) {
		if (isCanBeDirty()) {
			// Changed?  Priority order - don't drop back down.
			boolean changed = false;
			if (state!=dirtyState) {
				if (dirtyState==DirtyState.DIRTY && state==DirtyState.METADATA) {
					// Its too late to figure this out the other way round.
				} else {
					changed = true;
				}
			}
			
			if (changed) {
				dirtyState = state;
				
				for (Runnable r : dirtyChangeEvents) {
					r.run();
				}
			}
		}
		
		for (Runnable r : changeEvents) {
			r.run();
		}
	}
	
	public DirtyState getDirtyState () {
		return dirtyState;
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
	public List<MediaItem> getMediaTracks() {
		return Collections.unmodifiableList(mediaTracks);
	}
	
	protected void replaceList (List<MediaItem> newTracks) {
		List<MediaItem> tempList = new ArrayList<MediaItem>();
		
		for (MediaItem newItem : newTracks) {
			int indexOfOldItem = this.mediaTracks.indexOf(newItem);
			if (indexOfOldItem >= 0) {
				MediaItem oldItem = this.mediaTracks.get(indexOfOldItem);
				oldItem.setFromMediaItem(newItem);
				tempList.add(oldItem);
			} else {
				tempList.add(newItem);
			}
		}
		
		this.mediaTracks = tempList;
		setDirtyState(DirtyState.DIRTY);
	}
	
	public void addTrack (MediaItem track) {
		if (allowDuplicateEntries() || !mediaTracks.contains(track)) {
			mediaTracks.add(track);
			setDirtyState(DirtyState.DIRTY);
		}
	}
	
	public void removeMediaTrack (MediaItem track) throws MorriganException {
		mediaTracks.remove(track);
		setDirtyState(DirtyState.DIRTY);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Update methods.  Use these for data that is to be persisted.
//	These methods are sub-classed where persistance is needed.
	
	public void incTrackStartCnt (MediaItem track, long n) throws MorriganException {
		track.setStartCount(track.getStartCount() + n);
		setDirtyState(DirtyState.METADATA);
	}
	
	public void incTrackEndCnt (MediaItem track, long n) throws MorriganException {
		track.setEndCount(track.getEndCount() + n);
		setDirtyState(DirtyState.METADATA);
	}
	
	public void setDateAdded (MediaItem track, Date date) throws MorriganException {
		track.setDateAdded(date);
		setDirtyState(DirtyState.METADATA);
	}
	
	public void setDateLastPlayed (MediaItem track, Date date) throws MorriganException {
		track.setDateLastPlayed(date);
		setDirtyState(DirtyState.METADATA);
	}
	
	public void incTrackStartCnt (MediaItem track) throws MorriganException {
		track.setStartCount(track.getStartCount()+1);
		track.setDateLastPlayed(new Date());
		setDirtyState(DirtyState.METADATA);
	}
	
	public void incTrackEndCnt (MediaItem track) throws MorriganException {
		track.setEndCount(track.getEndCount()+1);
		setDirtyState(DirtyState.METADATA);
	}
	
	public void setTrackDuration (MediaItem track, int duration) throws MorriganException {
		track.setDuration(duration);
		setDirtyState(DirtyState.METADATA);
	}
	
	public void setTrackHashCode (MediaItem track, long hashcode) throws MorriganException {
		track.setHashcode(hashcode);
		setDirtyState(DirtyState.METADATA);
	}
	
	public void setTrackDateLastModified (MediaItem track, Date date) throws MorriganException {
		track.setDateLastModified(date);
		setDirtyState(DirtyState.METADATA);
	}
	
	public void setTrackEnabled (MediaItem track, boolean value) throws MorriganException {
		track.setEnabled(value);
		setDirtyState(DirtyState.METADATA);
	}
	
	public void setTrackMissing (MediaItem track, boolean value) throws MorriganException {
		track.setMissing(value);
		setDirtyState(DirtyState.METADATA);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Metadata readers.
	
	static public class DurationData {
		public long duration;
		public boolean complete;
	}
	
	public DurationData getTotalDuration () {
		DurationData ret = new DurationData();
		ret.complete = true;
		for (MediaItem mt : mediaTracks) {
			if (mt.getDuration() > 0) {
				ret.duration = ret.duration + mt.getDuration();
			} else {
				ret.complete = false;
			}
		}
		return ret;
	}
	
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString () {
		return listName + " ("+mediaTracks.size()+" items)";
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
