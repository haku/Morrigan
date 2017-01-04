package com.vaguehope.morrigan.player.internal;

import java.io.File;
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
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.MediaItemListChangeListener;
import com.vaguehope.morrigan.player.AbstractPlayer;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.LocalPlayerSupport;
import com.vaguehope.morrigan.player.OrderHelper;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;

public class LocalPlayerImpl extends AbstractPlayer implements LocalPlayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected final Logger logger = Logger.getLogger(this.getClass().getName());

	final LocalPlayerSupport localPlayerSupport;
	private final PlaybackEngineFactory playbackEngineFactory;
	private final ExecutorService executorService;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Main.

	public LocalPlayerImpl (final String id, final String name, final LocalPlayerSupport localPlayerSupport,
			final PlayerRegister register,
			final PlaybackEngineFactory playbackEngineFactory,
			final ExecutorService executorService) {
		super(id, name, register);
		this.localPlayerSupport = localPlayerSupport;
		this.playbackEngineFactory = playbackEngineFactory;
		this.executorService = executorService;
	}

	@Override
	protected void onDispose () {
		setCurrentItem(null);
		finalisePlaybackEngine();
	}

	@Override
	public boolean isProxy () {
		return false;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Current selection.

	private final Object _currentItemLock = new Object();
	private volatile PlayItem _currentItem = null;

	/**
	 * This is called at the start of each track.
	 * Must call this with a null before this
	 * object is disposed so as to remove listener.
	 */
	@Override
	public void setCurrentItem (final PlayItem item) {
		synchronized (this._currentItemLock) {
			if (this._currentItem != null && this._currentItem.hasList()) {
				this._currentItem.getList().removeChangeEventListener(this.listChangedRunnable);
			}

			this._currentItem = item;

			if (this._currentItem != null && this._currentItem.hasList()) {
				this._currentItem.getList().addChangeEventListener(this.listChangedRunnable);
				if (this._currentItem.hasTrack()) addToHistory(this._currentItem);
			}

			getListeners().currentItemChanged(item);
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

		final PlayItem currentItem = getCurrentItem();
		if (currentItem != null && currentItem.hasList()) {
			ret = currentItem.getList();
		}
		else {
			ret = this.localPlayerSupport.getCurrentList();
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Track order methods.

	PlayItem getNextItemToPlay () {
		final PlayItem queueItem = this.getQueue().takeFromQueue();
		if (queueItem != null) return queueItem;

		final PlayItem currentItem = getCurrentItem();
		final PlaybackOrder playbackOrder = getPlaybackOrder();

		if (currentItem != null && currentItem.isComplete()) {
			final IMediaTrack nextTrack = OrderHelper.getNextTrack(currentItem.getList(), currentItem.getTrack(), playbackOrder);
			if (nextTrack != null) {
				return new PlayItem(currentItem.getList(), nextTrack);
			}
			this.logger.info(String.format("OrderHelper.getNextTrack(%s,%s,%s) == null.",
					currentItem.getList(), currentItem.getTrack(), playbackOrder));
		}

		final IMediaTrackList<? extends IMediaTrack> currentList = getCurrentList();
		final IMediaTrack nextTrack = OrderHelper.getNextTrack(currentList, null, playbackOrder);
		if (nextTrack != null) {
			return new PlayItem(currentList, nextTrack);
		}
		this.logger.info(String.format("OrderHelper.getNextTrack(%s,%s,%s) == null.",
				currentList, null, playbackOrder));

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
			this.localPlayerSupport.historyChanged();
		}
	}

	void validateHistory () {
		synchronized (this._history) {
			boolean changed = false;

			for (int i = this._history.size() - 1; i >= 0; i--) {
				if (!this._history.get(i).getList().getMediaItems().contains(this._history.get(i).getTrack())) {
					this._history.remove(this._history.get(i));
					changed = true;
				}
			}

			if (changed) {
				this.localPlayerSupport.historyChanged();
			}
		}
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
			} catch (final PlaybackException e) {
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

	@Override
	protected void loadAndPlay (final PlayItem item, final File altFile) throws MorriganException {
		final File file = altFile != null ? altFile : new File(item.getTrack().getFilepath());

		// TODO FIXME Might be passed a remote file, but not supported yet.
		// To check it is a valid local file.
		if (!file.exists()) throw new MorriganException("File not found for item " + item + ": " + file.getAbsolutePath());

		final IPlaybackEngine engine = getPlaybackEngine(true);
		synchronized (engine) {
			this.logger.fine("Loading '" + item.getTrack().getTitle() + "'...");
			setCurrentItem(item);

			engine.setFile(file.getAbsolutePath());
			engine.setVideoFrameParent(this.localPlayerSupport.getCurrentMediaFrameParent());
			engine.loadTrack();
			engine.startPlaying();

			this._currentTrackDuration = engine.getDuration();
			this.logger.fine("Started to play '" + item.getTrack().getTitle() + "'...");

			// Put DB stuff in DB thread.
			this.executorService.submit(new Runnable() {
				@Override
				public void run () {
					try {
						item.getList().incTrackStartCnt(item.getTrack());
						getListeners().currentItemChanged(item);
						saveState();
					}
					catch (final MorriganException e) {
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

	/**
	 * For UI handlers to call.
	 */
	@Override
	public void pausePlaying () {
		try {
			internal_pausePlaying();
		} catch (final MorriganException e) {
			getListeners().onException(e);
		}
	}

	/**
	 * For UI handlers to call.
	 */
	@Override
	public void stopPlaying () {
		try {
			internal_stopPlaying();
		} catch (final MorriganException e) {
			getListeners().onException(e);
		}
	}

	@Override
	public void nextTrack () {
		final PlayItem nextItemToPlay = getNextItemToPlay();
		if (nextItemToPlay != null) {
//			stopPlaying(); // Is this really needed?
			loadAndStartPlaying(nextItemToPlay);
		}
	}

	@Override
	public PlayState getEnginePlayState () {
		final IPlaybackEngine eng = getPlaybackEngine(false);
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
		} catch (final MorriganException e) {
			getListeners().onException(e);
		}
	}

	private void internal_pausePlaying () throws MorriganException {
		final IPlaybackEngine eng = getPlaybackEngine(true);
		synchronized (eng) {
			final PlayState playbackState = eng.getPlaybackState();
			if (playbackState == PlayState.PAUSED) {
				eng.resumePlaying();
			}
			else if (playbackState == PlayState.PLAYING) {
				eng.pausePlaying();
			}
			else if (playbackState == PlayState.STOPPED) {
				final PlayItem ci = getCurrentItem();
				if (ci != null) loadAndStartPlaying(ci);
			}
			else {
				getListeners().onException(new PlaybackException("Don't know what to do.  Playstate=" + playbackState + "."));
			}
		} // END synchronized.
		this.getListeners().playStateChanged(getPlayState());
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
		final IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng != null) {
			synchronized (eng) {
				eng.stopPlaying();
				eng.unloadFile();
			}
			getListeners().playStateChanged(PlayState.STOPPED);
		}
	}

	protected void internal_seekTo (final double d) throws MorriganException {
		final IPlaybackEngine eng = getPlaybackEngine(false);
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
			getListeners().positionChanged(position, getCurrentTrackDuration());
		}

		@Override
		public void durationChanged(final int duration) {
			LocalPlayerImpl.this._currentTrackDuration = duration;

			if (duration > 0) {
				final PlayItem c = getCurrentItem();
				if (c != null && c.isComplete()) {
					if (c.getTrack().getDuration() != duration) {
						try {
							LocalPlayerImpl.this.logger.fine("setting item duration=" + duration);
							c.getList().setTrackDuration(c.getTrack(), duration);
						}
						catch (final MorriganException e) {
							LocalPlayerImpl.this.logger.log(Level.WARNING, "Failed to update track duration.", e);
						}
					}
				}
			}

			getListeners().positionChanged(getCurrentPosition(), duration);
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
				getCurrentItem().getList().incTrackEndCnt(getCurrentItem().getTrack());
			} catch (final MorriganException e) {
				getListeners().onException(e);
			}

			// Play next track?
			final PlayItem nextItemToPlay = getNextItemToPlay();
			if (nextItemToPlay != null) {
				loadAndStartPlaying(nextItemToPlay);
			}
			else {
				LocalPlayerImpl.this.logger.info("No more tracks to play.");
				getListeners().currentItemChanged(null);
			}
		}

		@Override
		public void onError(final Exception e) {
			getListeners().onException(e);
		}

		@Override
		public void onKeyPress(final int keyCode) {
			if (keyCode == SWT.ESC) {
				LocalPlayerImpl.this.localPlayerSupport.videoAreaClose();
			}
		}

		@Override
		public void onMouseClick(final int button, final int clickCount) {
			LocalPlayerImpl.this.logger.info("Mouse click "+button+"*"+clickCount);
			if (clickCount > 1) {
				LocalPlayerImpl.this.localPlayerSupport.videoAreaSelected();
			}
		}

	};

	@Override
	public void setVideoFrameParent(final Composite cmfp) {
		final IPlaybackEngine engine = getPlaybackEngine(false);
		synchronized (engine) {
			engine.setVideoFrameParent(cmfp);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public Map<Integer, String> getMonitors() {
		return this.localPlayerSupport.getMonitors();
	}

	@Override
	public void goFullscreen(final int monitor) {
		this.localPlayerSupport.goFullscreen(monitor);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}