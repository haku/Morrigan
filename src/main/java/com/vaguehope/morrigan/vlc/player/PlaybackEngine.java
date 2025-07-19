package com.vaguehope.morrigan.vlc.player;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.IPlaybackStatusListener;
import com.vaguehope.morrigan.engines.playback.PlaybackException;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.InfoApi;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaEventListener;
import uk.co.caprica.vlcj.media.Meta;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

class PlaybackEngine implements IPlaybackEngine {

	private static final Logger LOG = LoggerFactory.getLogger(PlaybackEngine.class);

	private final AtomicBoolean atEos = new AtomicBoolean();
	private final AtomicBoolean stopPlaying = new AtomicBoolean();

	private final MediaPlayerFactory vlcFactory;
	private final Executor executor;
	private final Consumer<MediaPlayer> prepPlayer;

	private String m_filepath = null;
	private IPlaybackStatusListener m_listener = null;
	private PlayState m_playbackState = PlayState.STOPPED;

	public PlaybackEngine (final MediaPlayerFactory vlcFactory, final Executor executor, final Consumer<MediaPlayer> prepPlayer) {
		this.vlcFactory = vlcFactory;
		this.executor = executor;
		this.prepPlayer = prepPlayer;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	IPlaybackEngine methods.

	@Override
	public int readFileDuration(final String fpath) throws PlaybackException {
		try {
			return (int) TimeUnit.MILLISECONDS.toSeconds(Metadata.getDurationMilliseconds(this.vlcFactory, fpath));
		}
		catch (InterruptedException e) {
			throw new PlaybackException(e);
		}
	}

	@Override
	public void setFile(String filepath) {
		this.m_filepath = filepath;
	}

	@Override
	public void unloadFile() {
		finalisePlayback();
	}

	@Override
	public void finalise() {
		finalisePlayback();
	}

	@Override
	public void loadTrack() throws PlaybackException {
		loadFile();
	}

	@Override
	public void startPlaying() throws PlaybackException {
		startTrack();
	}

	@Override
	public void stopPlaying() throws PlaybackException {
		stopTrack();
	}

	@Override
	public void pausePlaying() throws PlaybackException {
		pauseTrack();
	}

	@Override
	public void resumePlaying() throws PlaybackException {
		resumeTrack();
	}

	@Override
	public PlayState getPlaybackState() {
		return this.m_playbackState;
	}

	@Override
	public int getDuration() throws PlaybackException {
		AudioPlayerComponent player = this.playerRef.get();
		return (int) (player.mediaPlayer().status().length() / 1000);
	}

	@Override
	public long getPlaybackProgress() throws PlaybackException {
		AudioPlayerComponent player = this.playerRef.get();
		return (int) (player.mediaPlayer().status().time() / 1000);
	}

	@Override
	public void seekTo(double d) throws PlaybackException {
		this.playLock.lock();
		try {
			AudioPlayerComponent player = this.playerRef.get();
			if (player != null) {
				player.mediaPlayer().controls().setPosition((float) d);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	@Override
	public void setPositionMillis(final long millis) {
		this.playLock.lock();
		try {
			AudioPlayerComponent player = this.playerRef.get();
			if (player != null) {
				player.mediaPlayer().controls().setTime(millis);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.m_listener = listener;
	}

	@Override
	public int getVolume() {
		final AudioPlayerComponent player = this.playerRef.get();
		if (player == null) return -1;
		return player.mediaPlayer().audio().volume();
	}

	@Override
	public int getVolumeMaxValue() {
		return 200;
	}

	@Override
	public void setVolume(int newVolume) {
		final AudioPlayerComponent player = this.playerRef.get();
		if (player != null) player.mediaPlayer().audio().setVolume(newVolume);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local play-back methods.

	private Lock playLock = new ReentrantLock();
	protected AtomicReference<AudioPlayerComponent> playerRef = new AtomicReference<>();

	private void finalisePlayback () {
		this.playLock.lock();
		try {
			AudioPlayerComponent player = this.playerRef.get();
			if (player != null) {
				player.mediaPlayer().controls().stop();
				player.release();
				this.playerRef.set(null);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	private void loadFile () throws PlaybackException {
		this.playLock.lock();
		try {
			setStateAndCallListener(PlayState.LOADING, false);
			AudioPlayerComponent player = this.playerRef.get();
			if (player == null) {
				player = new AudioPlayerComponent(this.vlcFactory);
				if (this.prepPlayer != null) this.prepPlayer.accept(player.mediaPlayer());
				this.playerRef.set(player);
				player.mediaPlayer().events().addMediaPlayerEventListener(this.mediaPlayerEventListener);
				player.mediaPlayer().events().addMediaEventListener(this.mediaEventListener);
			}
			else {
				player.mediaPlayer().controls().stop();
			}

			if (!player.mediaPlayer().media().startPaused(this.m_filepath)) {
				throw new IllegalStateException("startPaused() failed.");
			}
		}
		catch (Exception e) { // NOSONAR Report any error while loading files.
			setStateAndCallListener(PlayState.STOPPED, false);
			throw new PlaybackException("Failed to load '"+this.m_filepath+"'.", e);
		}
		finally {
			this.playLock.unlock();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Event handlers.

	private final MediaPlayerEventListener mediaPlayerEventListener = new MediaPlayerEventAdapter () {

		@Override
		public void finished(MediaPlayer mediaPlayer) {
			final InfoApi info = mediaPlayer.media().info();
			LOG.info("VLC finished: {}", info != null ? info.mrl() : null);

			if (mediaPlayer == PlaybackEngine.this.playerRef.get().mediaPlayer()) {
				PlaybackEngine.this.executor.execute(() -> handleEosEvent());
			}
		}

		@Override
		public void stopped(MediaPlayer mediaPlayer) {
			PlaybackEngine.this.executor.execute(() -> setStateAndCallListener(PlayState.STOPPED, false));
		}
		@Override
		public void playing(MediaPlayer mediaPlayer) {
			PlaybackEngine.this.executor.execute(() -> setStateAndCallListener(PlayState.PLAYING, false));
		}
		@Override
		public void paused(MediaPlayer mediaPlayer) {
			PlaybackEngine.this.executor.execute(() -> setStateAndCallListener(PlayState.PAUSED, false));
		}

		@Override
		public void timeChanged (MediaPlayer mediaPlayer, long newTime) {
			PlaybackEngine.this.executor.execute(() -> callPositionListener(newTime / 1000));
		}

		@Override
		public void lengthChanged (MediaPlayer mediaPlayer, long newLength) {
			PlaybackEngine.this.executor.execute(() -> callDurationListener((int) (newLength / 1000)));
		}

		@Override
		public void error (MediaPlayer mediaPlayer) {
			// TODO work out what to do with this.  call m_listener ?
		}

	};

	private final MediaEventListener mediaEventListener = new MediaEventAdapter() {
		@Override
		public void mediaMetaChanged(final Media media, final Meta metaType) {
			final String newVal = media.meta().get(metaType);
			if (newVal != null) LOG.info("metaChanged {}:", metaType , newVal);
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Control methods.

	private void startTrack () {
		this.playLock.lock();
		try {
			this.stopPlaying.set(false);
			this.atEos.set(false);

			AudioPlayerComponent player = this.playerRef.get();
			if (player != null) {
				player.mediaPlayer().controls().play(); // This is an async call.
//				setStateAndCallListener(PlayState.Playing); // Do think this is needed as there will be a call back.
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	private void pauseTrack () {
		this.playLock.lock();
		try {
			AudioPlayerComponent player = this.playerRef.get();
			if (player != null) {
				player.mediaPlayer().controls().setPause(true);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	private void resumeTrack () {
		this.playLock.lock();
		try {
			AudioPlayerComponent player = this.playerRef.get();
			if (player != null) {
				/* Using play() instead of setPause(false) as it seems safer.
				 * May change it if it causes issues.
				 */
				player.mediaPlayer().controls().play();
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	private void stopTrack () {
		this.playLock.lock();
		try {
			this.stopPlaying.set(true);
			AudioPlayerComponent player = this.playerRef.get();
			if (player != null) {
				player.mediaPlayer().controls().stop();
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	void handleEosEvent () {
		if (!this.stopPlaying.get()) {
			if (this.atEos.compareAndSet(false, true)) {
				callOnEndOfTrackHandler();
			}
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Listener helper methods.

	private void callOnEndOfTrackHandler () {
		setStateAndCallListener(PlayState.STOPPED, true);
	}

	void setStateAndCallListener (PlayState state, final boolean isEndOfTrack) {
		this.m_playbackState = state;
		if (this.m_listener != null) this.m_listener.statusChanged(state, isEndOfTrack);
	}

	void callPositionListener(long position) {
		if (this.m_listener != null) this.m_listener.positionChanged(position);
	}

	void callDurationListener(int duration) {
		if (this.m_listener != null) this.m_listener.durationChanged(duration);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
