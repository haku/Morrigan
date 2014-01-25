package com.vaguehope.morrigan.player.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.engines.common.ImplException;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.engines.playback.IPlaybackStatusListener;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.engines.playback.PlaybackException;
import com.vaguehope.morrigan.model.Register;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.MediaItemListChangeListener;
import com.vaguehope.morrigan.player.DefaultPlayerQueue;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.OrderHelper;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerEventHandler;
import com.vaguehope.morrigan.player.PlayerQueue;

public class LocalPlayerImpl implements LocalPlayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final int id;
	private final String name;

	protected final Logger logger = Logger.getLogger(this.getClass().getName());

	final PlayerEventHandler eventHandler;
	private final Register<Player> register;
	private final PlaybackEngineFactory playbackEngineFactory;
	private final ExecutorService executorService;
	private final PlayerQueue queue;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Main.

	public LocalPlayerImpl (final int id, final String name, final PlayerEventHandler eventHandler,
			final Register<Player> register,
			final PlaybackEngineFactory playbackEngineFactory,
			final ExecutorService executorService) {
		this.id = id;
		this.name = name;
		this.eventHandler = eventHandler;
		this.register = register;
		this.playbackEngineFactory = playbackEngineFactory;
		this.executorService = executorService;
		this.queue = new DefaultPlayerQueue();
	}

	@Override
	public void dispose () {
		this.register.unregister(this);
		setCurrentItem(null);
		finalisePlaybackEngine();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ID.

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public String getName() {
		return this.name;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Current selection.

	private final Object _currentItemLock = new Object();
	private PlayItem _currentItem = null;

	/**
	 * This is called at the start of each track.
	 * Must call this with list a null before this
	 * object is disposed so as to remove listener.
	 */
	private void setCurrentItem (final PlayItem item) {
		synchronized (this._currentItemLock) {
			if (this._currentItem != null && this._currentItem.list != null) {
				this._currentItem.list.removeChangeEventListener(this.listChangedRunnable);
			}

			this._currentItem = item;

			if (this._currentItem != null && this._currentItem.list != null) {
				this._currentItem.list.addChangeEventListener(this.listChangedRunnable);

				if (this._currentItem.item != null) {
					addToHistory(this._currentItem);
				}
			}

			this.eventHandler.currentItemChanged();
		}
	}

	private final MediaItemListChangeListener listChangedRunnable = new MediaItemListChangeListener () {

		@Override
		public void mediaItemsRemoved (final IMediaItem... items) {
			validateHistory(); // TODO should this be scheduled / rate limited?
		}

		@Override
		public void eventMessage(final String msg) { /* Unused. */ }
		@Override
		public void mediaListRead() { /* Unused. */ }
		@Override
		public void dirtyStateChanged (final DirtyState oldState, final DirtyState newState) { /* Unused. */ }
		@Override
		public void mediaItemsAdded (final IMediaItem... items) { /* Unused. */ }
		@Override
		public void mediaItemsUpdated (final IMediaItem... items) { /* Unused. */ }
		@Override
		public void mediaItemsForceReadRequired(final IMediaItem... items) { /* Unused. */ }
	};

	@Override
	public PlayItem getCurrentItem () {
		// TODO check item is still valid.
		return this._currentItem;
	}

	@Override
	public IMediaTrackList<? extends IMediaTrack> getCurrentList () {
		IMediaTrackList<? extends IMediaTrack> ret = null;

		PlayItem currentItem = getCurrentItem();
		if (currentItem != null && currentItem.list != null) {
			ret = currentItem.list;
		}
		else {
			ret = this.eventHandler.getCurrentList();
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Track order methods.

	private PlaybackOrder _playbackOrder = PlaybackOrder.SEQUENTIAL;

	@Override
	public PlaybackOrder getPlaybackOrder () {
		return this._playbackOrder;
	}

	@Override
	public void setPlaybackOrder (final PlaybackOrder order) {
		this._playbackOrder = order;
	}

	PlayItem getNextItemToPlay () {
		final PlayItem queueItem = this.queue.takeFromQueue();
		if (queueItem != null) return queueItem;

		if (getCurrentItem() != null && getCurrentItem().list != null) {
			if (getCurrentItem().item != null) {
				IMediaTrack nextTrack = OrderHelper.getNextTrack(getCurrentItem().list, getCurrentItem().item, this._playbackOrder);
				if (nextTrack != null) {
					return new PlayItem(getCurrentItem().list, nextTrack);
				}
			}
		}
		else {
			IMediaTrackList<? extends IMediaTrack> currentList = getCurrentList();
			if (currentList != null) {
				IMediaTrack nextTrack = OrderHelper.getNextTrack(currentList, null, this._playbackOrder);
				if (nextTrack != null) {
					return new PlayItem(currentList, nextTrack);
				}
			}
		}

		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	History.

	private static final int HISTORY_LENGTH = 10;

	private final List<PlayItem> _history = new ArrayList<PlayItem>();

	@Override
	public List<PlayItem> getHistory () {
		return Collections.unmodifiableList(this._history);
	}

	private void addToHistory (final PlayItem item) {
		synchronized (this._history) {
			if (this._history.contains(item)) {
				this._history.remove(item);
			}
			this._history.add(0, item);
			if (this._history.size() > HISTORY_LENGTH) {
				this._history.remove(this._history.size()-1);
			}
			this.eventHandler.historyChanged();
		}
	}

	void validateHistory () {
		synchronized (this._history) {
			boolean changed = false;

			for (int i = this._history.size() - 1; i >= 0; i--) {
				if (!this._history.get(i).list.getMediaItems().contains(this._history.get(i).item)) {
					this._history.remove(this._history.get(i));
					changed = true;
				}
			}

			if (changed) {
				this.eventHandler.historyChanged();
			}
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queue.

	@Override
	public PlayerQueue getQueue () {
		return this.queue;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Playback engine.

	IPlaybackEngine playbackEngine = null;

	@Override
	public boolean isPlaybackEngineReady () {
		return (this.playbackEngine != null);
	}

	private synchronized IPlaybackEngine getPlaybackEngine (final boolean create) {
		if (this.playbackEngine == null && create) {
			this.playbackEngine = this.playbackEngineFactory.newPlaybackEngine();
			if (this.playbackEngine == null) throw new RuntimeException("Failed to create playback engine instance.");
			this.playbackEngine.setStatusListener(this.playbackStatusListener);
		}

		return this.playbackEngine;
	}

	private void finalisePlaybackEngine () {
		IPlaybackEngine eng = null;

		eng = getPlaybackEngine(false);

		if (eng!=null) {
			try {
				eng.stopPlaying();
			} catch (PlaybackException e) {
				e.printStackTrace();
			}
			eng.unloadFile();
			eng.finalise();
		}
	}


//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Playback management.

	long _currentPosition = -1; // In seconds.
	int _currentTrackDuration = -1; // In seconds.

	/**
	 * For UI handlers to call.
	 */
	@Override
	public void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list) {
		IMediaTrack nextTrack = OrderHelper.getNextTrack(list, null, this._playbackOrder);
		loadAndStartPlaying(list, nextTrack);
	}

	/**
	 * For UI handlers to call.
	 */
	@Override
	public void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack track) {
		if (track == null) throw new NullPointerException();
		loadAndStartPlaying(new PlayItem(list, track));
	}

	/**
	 * For UI handlers to call.
	 */
	@Override
	public void loadAndStartPlaying (final PlayItem item) {
		try {
			if (item.list == null) throw new IllegalArgumentException("PlayItem list can not be null.");

			if (item.item == null) {
				item.item = OrderHelper.getNextTrack(item.list, null, this._playbackOrder);
			}

			if (!item.item.isPlayable()) throw new IllegalArgumentException("Item is not playable: '"+item.item.getFilepath()+"'.");

			File file = new File(item.item.getFilepath());
			if (!file.exists()) throw new FileNotFoundException(item.item.getFilepath());

			IPlaybackEngine engine = getPlaybackEngine(true);
			synchronized (engine) {
				this.logger.fine("Loading '" + item.item.getTitle() + "'...");
				setCurrentItem(item);

				engine.setFile(item.item.getFilepath());
				engine.setVideoFrameParent(this.eventHandler.getCurrentMediaFrameParent());
				engine.loadTrack();
				engine.startPlaying();

				this._currentTrackDuration = engine.getDuration();
				this.logger.fine("Started to play '" + item.item.getTitle() + "'...");

				// Put DB stuff in DB thread.
				this.executorService.submit(new Runnable() {
					@Override
					public void run () {
						try {
							item.list.incTrackStartCnt(item.item);
							LocalPlayerImpl.this.eventHandler.currentItemChanged();
						}
						catch (MorriganException e) {
							LocalPlayerImpl.this.logger.log(Level.WARNING, "Failed to increment track count.", e);
						}
					}
				});

				/* This was useful at some point, but leaving it disabled for now.
				 * Will put it back if it proves needed.
				 */
//				if (item.item.getDuration() <= 0 && Player.this._currentTrackDuration > 0) {
//					item.list.setTrackDuration(item.item, Player.this._currentTrackDuration);
//				}

			} // END synchronized.
		}
		catch (Exception e) {
			this.eventHandler.asyncThrowable(e);
		}

		this.eventHandler.updateStatus();
	}

	/**
	 * For UI handlers to call.
	 */
	@Override
	public void pausePlaying () {
		try {
			internal_pausePlaying();
		} catch (MorriganException e) {
			this.eventHandler.asyncThrowable(e);
		}
	}

	/**
	 * For UI handlers to call.
	 */
	@Override
	public void stopPlaying () {
		try {
			internal_stopPlaying();
		} catch (MorriganException e) {
			this.eventHandler.asyncThrowable(e);
		}
	}

	@Override
	public void nextTrack () {
		PlayItem nextItemToPlay = getNextItemToPlay();
		if (nextItemToPlay != null) {
//			stopPlaying(); // Is this really needed?
			loadAndStartPlaying(nextItemToPlay);
		}
	}

	@Override
	public PlayState getPlayState () {
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			return eng.getPlaybackState();
		}
		return PlayState.STOPPED;
	}

	@Override
	public long getCurrentPosition () {
		return this._currentPosition;
	}

	@Override
	public int getCurrentTrackDuration () {
		return this._currentTrackDuration;
	}

	@Override
	public void seekTo (final double d) {
		try {
			internal_seekTo(d);
		} catch (MorriganException e) {
			this.eventHandler.asyncThrowable(e);
		}
	}

	private void internal_pausePlaying () throws MorriganException {
		// Don't go and make a player engine instance.
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			synchronized (eng) {
				PlayState playbackState = eng.getPlaybackState();
				if (playbackState == PlayState.PAUSED) {
					eng.resumePlaying();
				}
				else if (playbackState == PlayState.PLAYING) {
					eng.pausePlaying();
				}
				else if (playbackState == PlayState.STOPPED) {
					loadAndStartPlaying(getCurrentItem());
				}
				else {
					this.eventHandler.asyncThrowable(new PlaybackException("Don't know what to do.  Playstate=" + playbackState + "."));
				}
			} // END synchronized.
			this.eventHandler.updateStatus();
		}
	}

	/**
	 * For internal use.  Does not update GUI.
	 * @throws ImplException
	 * @throws PlaybackException
	 */
	private void internal_stopPlaying () throws ImplException, PlaybackException {
		/* Don't go and make a player engine instance
		 * just to call stop on it.
		 */
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			synchronized (eng) {
				eng.stopPlaying();
				eng.unloadFile();
			}
			this.eventHandler.updateStatus();
		}
	}

	protected void internal_seekTo (final double d) throws MorriganException {
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			synchronized (eng) {
				eng.seekTo(d);
			}
		}
	}

	private final IPlaybackStatusListener playbackStatusListener = new IPlaybackStatusListener () {

		@Override
		public void positionChanged(final long position) {
			LocalPlayerImpl.this._currentPosition = position;
			LocalPlayerImpl.this.eventHandler.updateStatus();
		}

		@Override
		public void durationChanged(final int duration) {
			LocalPlayerImpl.this._currentTrackDuration = duration;

			if (duration > 0) {
				PlayItem c = getCurrentItem();
				if (c != null && c.list != null && c.item != null) {
					if (c.item.getDuration() != duration) {
						try {
							LocalPlayerImpl.this.logger.fine("setting item duration=" + duration);
							c.list.setTrackDuration(c.item, duration);
						}
						catch (MorriganException e) {
							LocalPlayerImpl.this.logger.log(Level.WARNING, "Failed to update track duration.", e);
						}
					}
				}
			}

			LocalPlayerImpl.this.eventHandler.updateStatus();
		}

		@Override
		public void statusChanged(final PlayState state) {
			/* UNUSED */
		}

		@Override
		public void onEndOfTrack() {
			LocalPlayerImpl.this.logger.fine("Player received endOfTrack event.");
			// Inc. stats.
			try {
				getCurrentItem().list.incTrackEndCnt(getCurrentItem().item);
			} catch (MorriganException e) {
				LocalPlayerImpl.this.eventHandler.asyncThrowable(e);
			}

			// Play next track?
			PlayItem nextItemToPlay = getNextItemToPlay();
			if (nextItemToPlay != null) {
				loadAndStartPlaying(nextItemToPlay);
			}
			else {
				LocalPlayerImpl.this.logger.info("No more tracks to play.");
				LocalPlayerImpl.this.eventHandler.updateStatus();
			}
		}

		@Override
		public void onError(final Exception e) {
			LocalPlayerImpl.this.eventHandler.asyncThrowable(e);
		}

		@Override
		public void onKeyPress(final int keyCode) {
			if (keyCode == SWT.ESC) {
				LocalPlayerImpl.this.eventHandler.videoAreaClose();
			}
		}

		@Override
		public void onMouseClick(final int button, final int clickCount) {
			LocalPlayerImpl.this.logger.info("Mouse click "+button+"*"+clickCount);
			if (clickCount > 1) {
				LocalPlayerImpl.this.eventHandler.videoAreaSelected();
			}
		}

	};

	@Override
	public void setVideoFrameParent(final Composite cmfp) {
		IPlaybackEngine engine = getPlaybackEngine(false);
		synchronized (engine) {
			engine.setVideoFrameParent(cmfp);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public Map<Integer, String> getMonitors() {
		return this.eventHandler.getMonitors();
	}

	@Override
	public void goFullscreen(final int monitor) {
		this.eventHandler.goFullscreen(monitor);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}