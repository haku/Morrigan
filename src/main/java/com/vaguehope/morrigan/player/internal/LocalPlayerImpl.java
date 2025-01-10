package com.vaguehope.morrigan.player.internal;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.engines.playback.IPlaybackStatusListener;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.engines.playback.PlaybackException;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.player.AbstractPlayer;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.PlayerStateStorage;
import com.vaguehope.morrigan.player.contentproxy.ContentProxy;
import com.vaguehope.morrigan.util.MnLogger;

public class LocalPlayerImpl extends AbstractPlayer implements Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final MnLogger LOG = MnLogger.make(LocalPlayerImpl.class);

	private final PlaybackEngineFactory playbackEngineFactory;
	private final ContentProxy contentProxy;
	private final ExecutorService executorService;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Main.

	public LocalPlayerImpl(
			final String id,
			final String name,
			final PlayerRegister register,
			final PlaybackEngineFactory playbackEngineFactory,
			final ContentProxy contentProxy,
			final ExecutorService executorService,
			final PlayerStateStorage playerStateStorage,
			final Config config) {
		super(id, name, register, playerStateStorage, config);
		this.playbackEngineFactory = playbackEngineFactory;
		this.contentProxy = contentProxy;
		this.executorService = executorService;
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
				LOG.e("Failed top stop playback.", e);
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
		else if (item.getTrack().hasRemoteLocation()) {
			if (item.hasList()) {
				mediaLocation = item.getList().prepairRemoteLocation(item.getTrack(), this.contentProxy);
			}
			else {
				mediaLocation = item.getTrack().getRemoteLocation();
			}
		}
		else {
			mediaLocation = item.getTrack().getFilepath();
			if (!Files.isReadable(Paths.get(mediaLocation))) throw new MorriganException("File not found for item " + item + ": " + mediaLocation);
		}

		final IPlaybackEngine engine = getPlaybackEngine(true);
		synchronized (engine) {
			LOG.d("Loading '{}'...", item.getTrack().getTitle());
			setCurrentItem(item);

			engine.setFile(mediaLocation);
			engine.loadTrack();
			engine.startPlaying();

			this._currentTrackDuration = engine.getDuration();
			LOG.d("Started to play '{}'...", item.getTrack().getTitle());

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
						LOG.e("Failed to increment track count.", e);
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
							LOG.d("setting item duration={}.", duration);
							c.getList().setTrackDuration(c.getTrack(), duration);
						}
						catch (final MorriganException e) {
							LOG.e("Failed to update track duration.", e);
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
			LOG.d("Player received endOfTrack event.");
			// Inc. stats.
			try {
				getCurrentItem().getList().incTrackEndCnt(getCurrentItem().getTrack());
			} catch (final MorriganException e) {
				getListeners().onException(e);
			}

			// Play next track?
			try {
				final PlayItem nextItemToPlay = findNextItemToPlay();
				if (nextItemToPlay != null) {
					loadAndStartPlaying(nextItemToPlay);
				}
				else {
					LOG.i("No more tracks to play.");
					getListeners().currentItemChanged(null);
				}
			}
			catch (final MorriganException e) {
				getListeners().onException(e);
			}
		}

		@Override
		public void onError(final Exception e) {
			getListeners().onException(e);
		}

	};

}