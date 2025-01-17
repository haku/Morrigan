package com.vaguehope.morrigan.player;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.engines.playback.IPlaybackStatusListener;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.engines.playback.PlaybackException;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.contentproxy.ContentProxy;

public class LocalPlayer extends AbstractPlayer implements Player {

	private static final Logger LOG = LoggerFactory.getLogger(LocalPlayer.class);

	private final PlaybackEngineFactory playbackEngineFactory;
	private final ContentProxy contentProxy;

	private final AtomicLong playbackStartTime = new AtomicLong();

	public LocalPlayer(
			final String id,
			final String name,
			final PlayerRegister register,
			final MediaFactory mediaFactory,
			final PlaybackEngineFactory playbackEngineFactory,
			final ContentProxy contentProxy,
			final ScheduledExecutorService schEx,
			final PlayerStateStorage playerStateStorage,
			final Config config) {
		super(id, name, register, mediaFactory, schEx, playerStateStorage, config);
		this.playbackEngineFactory = playbackEngineFactory;
		this.contentProxy = contentProxy;
	}

	@Override
	protected void onDispose () {
		setCurrentItem(null);
		finalisePlaybackEngine();
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
			}
			catch (final PlaybackException e) {
				LOG.error("Failed top stop playback.", e);
			}
			eng.unloadFile();
			eng.finalise();
		}

		if (this.playbackEngineFactory != null) {
			this.playbackEngineFactory.dispose();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Playback management.

	long _currentPosition = -1; // In seconds.
	int _currentTrackDuration = -1; // In seconds.

	@Override
	protected void loadAndPlay (final PlayItem item) throws MorriganException {
		final String mediaLocation;
		if (item.hasAltFile()) {
			mediaLocation = item.getAltFile().getAbsolutePath();
			if (!Files.isReadable(Paths.get(mediaLocation))) throw new MorriganException("Alt file not found for item " + item + ": " + mediaLocation);
		}
		else if (item.getItem().hasRemoteLocation()) {
			if (item.hasList()) {
				mediaLocation = item.getList().prepairRemoteLocation(item.getItem(), this.contentProxy);
			}
			else {
				mediaLocation = item.getItem().getRemoteLocation();
			}
		}
		else {
			mediaLocation = item.getItem().getFilepath();
			if (!Files.isReadable(Paths.get(mediaLocation))) throw new MorriganException("File not found for item " + item + ": " + mediaLocation);
		}

		final IPlaybackEngine engine = getPlaybackEngine(true);
		synchronized (engine) {
			LOG.debug("Loading: {}", item.getItem().getTitle());
			final PlayItem prevItem = setCurrentItem(item);
			if (prevItem != null) recordPlaybackOver(prevItem, false);

			engine.setFile(mediaLocation);
			engine.loadTrack();
			engine.startPlaying();

			this._currentTrackDuration = engine.getDuration();
			LOG.debug("Started to play: {}", item.getItem().getTitle());

			this.playbackStartTime.set(System.currentTimeMillis());
			this.schEx.submit(() -> getListeners().currentItemChanged(item));
			this.playbackRecorder.recordStarted(item);
			this.schEx.submit(() -> saveState());
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
	public int getCurrentTrackDurationFromRenderer () {
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
	private void internal_stopPlaying () throws PlaybackException {
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
				getListeners().afterSeek();
			}
		}
	}

	private void recordPlaybackOver(final PlayItem item, final boolean completed) {
		final long startTime = this.playbackStartTime.getAndSet(0L);
		if (startTime <= 0L) return;
		this.playbackRecorder.recordCompleted(item, completed, startTime);
	}

	private final IPlaybackStatusListener playbackStatusListener = new IPlaybackStatusListener () {

		@Override
		public void positionChanged(final long position) {
			LocalPlayer.this._currentPosition = position;
			getListeners().positionChanged(position, getCurrentTrackDuration());
		}

		@Override
		public void durationChanged(final int duration) {
			LocalPlayer.this._currentTrackDuration = duration;

			if (duration > 0) {
				final PlayItem c = getCurrentItem();
				if (c != null && c.hasListAndItem()) {
					if (c.getItem().getDuration() != duration) {
						try {
							LOG.debug("setting item duration={}.", duration);
							c.getList().setTrackDuration(c.getItem(), duration);
						}
						catch (final MorriganException e) {
							LOG.error("Failed to update track duration.", e);
						}
					}
				}
			}

			getListeners().positionChanged(getCurrentPosition(), duration);
		}

		@Override
		public void statusChanged(final PlayState state, final boolean isEndOfTrack) {
			if (state == PlayState.STOPPED) {
				recordPlaybackOver(getCurrentItem(), isEndOfTrack);
			}

			if (isEndOfTrack) {
				LOG.debug("Player received endOfTrack event.");
				// Play next track?
				try {
					final PlayItem nextItemToPlay = findNextItemToPlay();
					if (nextItemToPlay != null) {
						loadAndStartPlaying(nextItemToPlay);
					}
					else {
						LOG.info("No more tracks to play.");
						getListeners().currentItemChanged(null);
					}
				}
				catch (final MorriganException e) {
					getListeners().onException(e);
				}
			}
		}

		@Override
		public void onError(final Exception e) {
			getListeners().onException(e);
		}

	};

}