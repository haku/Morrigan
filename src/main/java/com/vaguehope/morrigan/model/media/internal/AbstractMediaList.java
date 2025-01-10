package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.AbstractItem;
import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaListChangeListener;
import com.vaguehope.morrigan.util.FileHelper;

public abstract class AbstractMediaList extends AbstractList<AbstractItem> implements MediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	final Logger logger = Logger.getLogger(this.getClass().getName());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors and parameters.

	private final ListRef listRef;
	private final String listName;
	private final List<MediaItem> mediaTracks = new CopyOnWriteArrayList<>();

	/**
	 * listId must be unique. It will be used to identify the matching editor.
	 * @param listId
	 *            a unique ID.
	 * @param listName
	 *            a human-readable title for this list.
	 */
	protected AbstractMediaList (final ListRef listRef, final String listName) {
		if (listRef == null) throw new IllegalArgumentException("listRef can not be null.");
		if (listName == null) throw new IllegalArgumentException("listName can not be null.");

		this.listRef = listRef;
		this.listName = listName;
	}

	@Override
	public void dispose () {
		this.changeEventListeners.clear();
		synchronized (this.mediaTracks) {
			this.mediaTracks.clear();
		}
	}

	@Override
	public ListRef getListRef() {
		return this.listRef;
	}

	@Override
	public String getListName () {
		return this.listName;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Dirty state and event listeners.

	private volatile DirtyState dirtyState = DirtyState.CLEAN;
	protected final List<MediaListChangeListener> changeEventListeners = Collections.synchronizedList(new ArrayList<MediaListChangeListener>());

	public abstract boolean isCanBeDirty ();

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
	public void addChangeEventListener (final MediaListChangeListener listener) {
		// TODO rewrite this to use a map instead?
		if (!this.changeEventListeners.contains(listener)) this.changeEventListeners.add(listener);
	}

	@Override
	public void removeChangeEventListener (final MediaListChangeListener listener) {
		this.changeEventListeners.remove(listener);
	}

	private final MediaListChangeListener changeCaller = new MediaListChangeListener() {

		@Override
		public void eventMessage (final String msg) {
			if (AbstractMediaList.this.logger.isLoggable(Level.FINEST)) AbstractMediaList.this.logger.finest(getListName() + " eventMessage=" + msg);
			for (final MediaListChangeListener listener : AbstractMediaList.this.changeEventListeners) {
				listener.eventMessage(msg);
			}
		}

		@Override
		public void dirtyStateChanged (final DirtyState oldState, final DirtyState newState) {
			if (AbstractMediaList.this.logger.isLoggable(Level.FINEST)) AbstractMediaList.this.logger.finest(getListName() + " oldState=" + oldState + " newState=" + newState);
			for (final MediaListChangeListener listener : AbstractMediaList.this.changeEventListeners) {
				listener.dirtyStateChanged(oldState, newState);
			}
		}

		@Override
		public void mediaListRead () {
			if (AbstractMediaList.this.logger.isLoggable(Level.FINEST)) AbstractMediaList.this.logger.finest(getListName());
			for (final MediaListChangeListener listener : AbstractMediaList.this.changeEventListeners) {
				listener.mediaListRead();
			}
		}

		@Override
		public void mediaItemsAdded (final MediaItem... items) {
			if (AbstractMediaList.this.logger.isLoggable(Level.FINEST)) AbstractMediaList.this.logger.finest(getListName() + " " + Arrays.toString(items));
			for (final MediaListChangeListener listener : AbstractMediaList.this.changeEventListeners) {
				listener.mediaItemsAdded(items);
			}
		}

		@Override
		public void mediaItemsRemoved (final MediaItem... items) {
			if (AbstractMediaList.this.logger.isLoggable(Level.FINEST)) AbstractMediaList.this.logger.finest(getListName() + " " + Arrays.toString(items));
			for (final MediaListChangeListener listener : AbstractMediaList.this.changeEventListeners) {
				listener.mediaItemsRemoved(items);
			}
		}

		@Override
		public void mediaItemsUpdated (final MediaItem... items) {
			if (AbstractMediaList.this.logger.isLoggable(Level.FINEST)) AbstractMediaList.this.logger.finest(getListName() + " " + Arrays.toString(items));
			for (final MediaListChangeListener listener : AbstractMediaList.this.changeEventListeners) {
				listener.mediaItemsUpdated(items);
			}
		}

		@Override
		public void mediaItemsForceReadRequired (final MediaItem... items) {
			if (AbstractMediaList.this.logger.isLoggable(Level.FINEST)) AbstractMediaList.this.logger.finest(getListName() + " " + Arrays.toString(items));
			for (final MediaListChangeListener listener : AbstractMediaList.this.changeEventListeners) {
				listener.mediaItemsForceReadRequired(items);
			}
		}

	};

	@Override
	public MediaListChangeListener getChangeEventCaller () {
		return this.changeCaller;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public abstract boolean allowDuplicateEntries ();

	/**
	 * Returns an unmodifiable list of the playlist items.
	 */
	@Override
	public List<MediaItem> getMediaItems () {
		synchronized (this.mediaTracks) {
			return Collections.unmodifiableList(this.mediaTracks);
		}
	}

	protected void setMediaTracks (final List<MediaItem> newMediaTracks) {
		synchronized (this.mediaTracks) {
			this.mediaTracks.clear();
			this.mediaTracks.addAll(newMediaTracks);
		}
		setDirtyState(DirtyState.DIRTY);
	}

	/**
	 * Use this variant when you are about to to re-query the DB anyway and
	 * don't want to do two successive updates.
	 * @param newTracks
	 * @return items that are removed.
	 */
	protected List<MediaItem> replaceListWithoutSetDirty (final List<MediaItem> newTracks) {
		synchronized (this.mediaTracks) {
			return updateList(this.mediaTracks, newTracks, false);
		}
	}

	@Override
	public void addItem (final MediaItem track) {
		boolean diritied = false;
		synchronized (this.mediaTracks) {
			if (allowDuplicateEntries() || !this.mediaTracks.contains(track)) {
				this.mediaTracks.add(track);
				getChangeEventCaller().mediaItemsAdded(track);
				diritied = true;
			}
		}
		if (diritied) setDirtyState(DirtyState.DIRTY);
	}

	@Override
	public void removeItem (final MediaItem track) throws MorriganException {
		boolean diritied = false;
		synchronized (this.mediaTracks) {
			this.mediaTracks.remove(track);
			getChangeEventCaller().mediaItemsRemoved(track);
			diritied = true;
		}
		if (diritied) setDirtyState(DirtyState.DIRTY);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Update methods.  Use these for data that is to be persisted.
//	These methods are sub-classed where persistence is needed.

	@Override
	public void setItemDateAdded (final MediaItem track, final Date date) throws MorriganException {
		track.setDateAdded(date);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}

	@Override
	public void setItemMd5 (final MediaItem track, final BigInteger md5) throws MorriganException {
		track.setMd5(md5);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}

	@Override
	public void setItemSha1 (final MediaItem track, final BigInteger sha1) throws MorriganException {
		track.setSha1(sha1);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}

	@Override
	public void setItemDateLastModified (final MediaItem track, final Date date) throws MorriganException {
		track.setDateLastModified(date);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}

	@Override
	public void setItemEnabled (final MediaItem track, final boolean value) throws MorriganException {
		track.setEnabled(value);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}

	@Override
	public void setItemEnabled (final MediaItem track, final boolean value, final Date lastModified) throws MorriganException {
		track.setEnabled(value, lastModified);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}

	@Override
	public void setItemMissing (final MediaItem track, final boolean value) throws MorriganException {
		track.setMissing(value);
		getChangeEventCaller().mediaItemsUpdated(track);
		setDirtyState(DirtyState.METADATA);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.

	@Override
	public void copyItemFile (final MediaItem item, final OutputStream os) throws MorriganException {
		// TODO implement this when it is needed.
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public File copyItemFile (final MediaItem mi, final File targetDirectory) throws MorriganException {
		if (!targetDirectory.isDirectory()) {
			throw new IllegalArgumentException("targetDirectory must be a directory.");
		}

		final File targetFile = new File(targetDirectory.getAbsolutePath() + File.separatorChar
				+ mi.getFilepath().substring(mi.getFilepath().lastIndexOf(File.separatorChar) + 1));

		if (!targetFile.exists()) {
			System.err.println("Copying '" + mi.getFilepath() + "' to '" + targetFile.getAbsolutePath() + "'...");
			try {
				FileHelper.copyFile(new File(mi.getFilepath()), targetFile);
			}
			catch (final IOException e) {
				throw new MorriganException(e);
			}
		}
		else {
			System.err.println("Skipping '" + targetFile.getAbsolutePath() + "' as it already exists.");
		}

		return targetFile;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String toString () {
		return this.listName + " (" + size() + " items)";
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Static helper methods.

	/**
	 * Update keepList without replacing any equivalent objects. Instead copy
	 * the data from the new object the old one. This is to work around how a
	 * GUI list uses a data provider.
	 *
	 * @param keepList
	 * @param freshList
	 * @return items that were removed from keepList.
	 */
	protected static List<MediaItem> updateList (final List<MediaItem> keepList, final List<MediaItem> freshList, final boolean updateKeepList) {
		final List<MediaItem> finalList = new ArrayList<>();

		// This block takes no time.
		Map<String, MediaItem> keepMap = new HashMap<>(keepList.size());
		for (final MediaItem e : keepList) {
			keepMap.put(e.getFilepath(), e);
		}

		// This block is very quick.
		if (freshList != null) for (final MediaItem newItem : freshList) {
			final MediaItem oldItem = keepMap.get(newItem.getFilepath());
			if (oldItem != null) {
				if (oldItem != newItem) oldItem.setFromMediaItem(newItem);
				finalList.add(oldItem);
			}
			else {
				finalList.add(newItem);
			}
		}

		/*
		 * Create a new list and populate it with the items removed.
		 */
		final List<MediaItem> removedItems = new ArrayList<>();
		keepMap = new HashMap<>(keepList.size());
		for (final MediaItem e : finalList) {
			keepMap.put(e.getFilepath(), e);
		}
		for (final MediaItem e : keepList) {
			if (!keepMap.containsKey(e.getFilepath())) {
				removedItems.add(e);
			}
		}

		/*
		 * Update the keep list. We need to modify the passed in list, not
		 * return a new one. This block takes no time.
		 */
		if (updateKeepList) {
			keepList.clear();
			keepList.addAll(finalList);
		}

		return removedItems;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public int size() {
		synchronized (this.mediaTracks) {
			return this.mediaTracks.size();
		}
	}

	@Override
	public AbstractItem get(final int index) {
		synchronized (this.mediaTracks) {
			return this.mediaTracks.get(index);
		}
	}

	@Override
	public int indexOf(final Object o) {
		synchronized (this.mediaTracks) {
			return this.mediaTracks.indexOf(o);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
