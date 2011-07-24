package net.sparktank.morrigan.model.media.internal;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.DirtyState;
import net.sparktank.morrigan.model.media.IMediaItem;
import net.sparktank.morrigan.model.media.IMediaItemList;
import net.sparktank.morrigan.model.media.MediaItemListChangeListener;
import net.sparktank.morrigan.util.FileHelper;

public abstract class MediaItemList<T extends IMediaItem> implements IMediaItemList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
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
	
	@Override
	public void dispose () {
		this.changeEventListeners.clear();
		this.mediaTracks.clear();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * A unique identifier.
	 */
	@Override
	public String getListId () {
		return this.listId;
	}
	
	/**
	 * A human readable name for the GUI.
	 * @return
	 */
	@Override
	public String getListName () {
		return this.listName;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	abstract public String getType ();
	
	@Override
	abstract public String getSerial ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Dirty state and event listeners.
	
	private DirtyState dirtyState = DirtyState.CLEAN;
	final protected List<MediaItemListChangeListener> changeEventListeners = new LinkedList<MediaItemListChangeListener>();
	
	abstract public boolean isCanBeDirty ();
	
	@Override
	public void setDirtyState (final DirtyState state) {
		final DirtyState oldState = this.dirtyState;
		if (isCanBeDirty()) {
			this.dirtyState = state;
			getChangeEventCaller().dirtyStateChanged(oldState, state);
		}
	}
	
	@Override
	public DirtyState getDirtyState () {
		return this.dirtyState;
	}
	
	@Override
	public void addChangeEventListener (MediaItemListChangeListener listener) {
		// TODO rewrite this to use a map instead?
		if (!this.changeEventListeners.contains(listener)) this.changeEventListeners.add(listener);
	}
	
	@Override
	public void removeChangeEventListener (MediaItemListChangeListener listener) {
		this.changeEventListeners.remove(listener);
	}
	
	private MediaItemListChangeListener changeCaller = new MediaItemListChangeListener () {
		
		@Override
		public void dirtyStateChanged(DirtyState oldState, DirtyState newState) {
			if (MediaItemList.this.logger.isLoggable(Level.FINEST)) MediaItemList.this.logger.finest(getListName() +  " oldState=" + oldState + " newState=" + newState);
			for (MediaItemListChangeListener listener : MediaItemList.this.changeEventListeners) {
				listener.dirtyStateChanged(oldState, newState);
			}
		}
		
		@Override
		public void mediaListRead() {
			if (MediaItemList.this.logger.isLoggable(Level.FINEST)) MediaItemList.this.logger.finest(getListName());
			for (MediaItemListChangeListener listener : MediaItemList.this.changeEventListeners) {
				listener.mediaListRead();
			}
		}
		
		@Override
		public void mediaItemsAdded(IMediaItem... items) {
			if (MediaItemList.this.logger.isLoggable(Level.FINEST)) MediaItemList.this.logger.finest(getListName() +  " " + Arrays.toString(items));
			for (MediaItemListChangeListener listener : MediaItemList.this.changeEventListeners) {
				listener.mediaItemsAdded(items);
			}
		}
		
		@Override
		public void mediaItemsRemoved(IMediaItem... items) {
			if (MediaItemList.this.logger.isLoggable(Level.FINEST)) MediaItemList.this.logger.finest(getListName() +  " " + Arrays.toString(items));
			for (MediaItemListChangeListener listener : MediaItemList.this.changeEventListeners) {
				listener.mediaItemsRemoved(items);
			}
		}
		
		@Override
		public void mediaItemsUpdated(IMediaItem... items) {
			if (MediaItemList.this.logger.isLoggable(Level.FINEST)) MediaItemList.this.logger.finest(getListName() +  " " + Arrays.toString(items));
			for (MediaItemListChangeListener listener : MediaItemList.this.changeEventListeners) {
				listener.mediaItemsUpdated(items);
			}
		}
		
		@Override
		public void mediaItemsForceReadRequired(IMediaItem... items) {
			if (MediaItemList.this.logger.isLoggable(Level.FINEST)) MediaItemList.this.logger.finest(getListName() +  " " + Arrays.toString(items));
			for (MediaItemListChangeListener listener : MediaItemList.this.changeEventListeners) {
				listener.mediaItemsForceReadRequired(items);
			}
		}
		
	};
	
	@Override
	public MediaItemListChangeListener getChangeEventCaller () {
		return this.changeCaller;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	abstract public boolean allowDuplicateEntries ();
	
	@Override
	abstract public void read () throws MorriganException;
	
	@Override
	public int getCount () {
		return this.mediaTracks.size();
	}
	
	/**
	 * Returns an unmodifiable list of the playlist items.
	 * @return
	 */
	@Override
	public List<T> getMediaItems() {
		return Collections.unmodifiableList(this.mediaTracks);
	}
	
	protected void setMediaTracks (List<T> newMediaTracks) {
		synchronized (this.mediaTracks) {
			this.mediaTracks.clear();
			this.mediaTracks.addAll(newMediaTracks);
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
	
	@Override
	public void addItem (T track) {
		if (allowDuplicateEntries() || !this.mediaTracks.contains(track)) {
			this.mediaTracks.add(track);
			getChangeEventCaller().mediaItemsAdded(track);
			setDirtyState(DirtyState.DIRTY);
		}
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void removeItem (T track) throws MorriganException {
		this.mediaTracks.remove(track);
		getChangeEventCaller().mediaItemsRemoved(track);
		setDirtyState(DirtyState.DIRTY);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Update methods.  Use these for data that is to be persisted.
//	These methods are sub-classed where persistence is needed.
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void setItemDateAdded (T track, Date date) throws MorriganException {
		track.setDateAdded(date);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void setItemHashCode (T track, BigInteger hashcode) throws MorriganException {
		track.setHashcode(hashcode);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void setItemDateLastModified (T track, Date date) throws MorriganException {
		track.setDateLastModified(date);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void setItemEnabled (T track, boolean value) throws MorriganException {
		track.setEnabled(value);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void setItemMissing (T track, boolean value) throws MorriganException {
		track.setMissing(value);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	@Override
	public File copyItemFile (T mi, File targetDirectory) throws MorriganException {
		if (!targetDirectory.isDirectory()) {
			throw new IllegalArgumentException("targetDirectory must be a directory.");
		}
		
		File targetFile = new File(targetDirectory.getAbsolutePath() + File.separatorChar
				+ mi.getFilepath().substring(mi.getFilepath().lastIndexOf(File.separatorChar) + 1));
		
		if (!targetFile.exists()) {
			System.err.println("Copying '"+mi.getFilepath()+"' to '"+targetFile.getAbsolutePath()+"'...");
			try {
				FileHelper.copyFile(new File(mi.getFilepath()), targetFile);
			} catch (IOException e) {
				throw new MorriganException(e);
			}
		}
		else {
			System.err.println("Skipping '"+targetFile.getAbsolutePath()+"' as it already exists.");
		}
		
		return targetFile;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queries.
	
	@Override
	public T findItemByFilePath (String path) {
		Map<String, T> map = new HashMap<String, T>();
		for (T item : this.mediaTracks) {
			map.put(item.getFilepath(), item);
		}
		return map.get(path);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString () {
		return this.listName + " ("+this.mediaTracks.size()+" items)";
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
