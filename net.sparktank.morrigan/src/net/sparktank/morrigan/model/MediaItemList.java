package net.sparktank.morrigan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sparktank.morrigan.exceptions.MorriganException;

public abstract class MediaItemList<T extends MediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public enum DirtyState { CLEAN, DIRTY, METADATA };
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors and parameters.
	
	private final String listId;
	private final String listName;
	private final List<T> mediaTracks = new ArrayList<T>();
	
	/**
	 * listId must be unique.  It will be used to identify
	 * the matching editor.
	 * @param listId a unique ID.
	 * @param listName a human-readable title for this list.
	 */
	protected MediaItemList (String listId, String listName) {
		if (listId == null) throw new IllegalArgumentException("listId can not be null.");
		if (listName == null) throw new IllegalArgumentException("listName can not be null.");
		
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
	public List<T> getMediaTracks() {
		return Collections.unmodifiableList(mediaTracks);
	}
	
	protected void setMediaTracks (List<T> newMediaTracks) {
		synchronized (mediaTracks) {
			mediaTracks.clear();
			mediaTracks.addAll(newMediaTracks);
		}
		setDirtyState(DirtyState.DIRTY);
	}
	
	/**
	 * 
	 * @param newTracks
	 * @return items that are removed.
	 */
	protected List<T> replaceList (List<T> newTracks) {
		List<T> ret = updateList(this.mediaTracks, newTracks);
		setDirtyState(DirtyState.DIRTY);
		return ret;
	}
	
	/**
	 * Use this variant when you are about to to re-query the DB anyway
	 * and don't want to do two successive updates. 
	 * @param newTracks
	 * @return items that are removed.
	 */
	protected List<T> replaceListWithoutSetDirty (List<T> newTracks) {
		return updateList(this.mediaTracks, newTracks, false);
	}
	
	public void addTrack (T track) {
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
	
	public void setDateAdded (MediaItem track, Date date) throws MorriganException {
		track.setDateAdded(date);
		setDirtyState(DirtyState.METADATA);
	}
	
	public void setDateLastPlayed (MediaItem track, Date date) throws MorriganException {
		track.setDateLastPlayed(date);
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
	
	@Override
	public String toString () {
		return listName + " ("+mediaTracks.size()+" items)";
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Typed helper methods.
	
	public List<T> updateList (List<T> keepList, List<T> freshList) {
		return updateList(keepList, freshList, true);
	}
	
	/**
	 * Update keepList without replacing any equivalent
	 * objects.  Instead copy the data from the new
	 * object the old one.  This is to work around how
	 * a GUI list uses a data provider.
	 * 
	 * @param keepList
	 * @param freshList
	 * @return items that were removed from keepList.
	 */
	public List<T> updateList (List<T> keepList, List<T> freshList, boolean UpdateKeepList) {
		List<T> finalList = new ArrayList<T>();
		
		synchronized (keepList) {
			synchronized (freshList) {
				
				// This block takes no time.
				Map<String,T> keepMap = new HashMap<String,T>(keepList.size());
				for (T e : keepList) {
					keepMap.put(e.getFilepath(), e);
				}
				
				// This block is very quick.
				for (T newItem : freshList) {
					T oldItem = keepMap.get(newItem.getFilepath());
					if (oldItem != null) {
						oldItem.setFromMediaItem(newItem);
						finalList.add(oldItem);
					} else {
						finalList.add(newItem);
					}
				}
				
				System.err.println("Replacing " + keepList.size() + " items with " + finalList.size() + " items.");
				
				/* Create a new list and populate it with the
				 * items removed.
				 */
				List<T> removedItems = new ArrayList<T>();
				keepMap = new HashMap<String,T>(keepList.size());
				for (T e : finalList) {
					keepMap.put(e.getFilepath(), e);
				}
				for (T e : keepList) {
					if (!keepMap.containsKey(e.getFilepath())) {
						removedItems.add(e);
					}
				}
				
				System.err.println("Removed " + removedItems.size() + " items.");
				
				/* Update the keep list.  We need to modify
				 * the passed in list, not return a new one.
				 * This block takes no time.
				 */
				if (UpdateKeepList) {
					keepList.clear();
					keepList.addAll(finalList);
				}
				
				return removedItems;
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
