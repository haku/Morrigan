package com.vaguehope.morrigan.player;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.Register;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.transcode.Transcode;
import com.vaguehope.morrigan.transcode.TranscodeProfile;
import com.vaguehope.morrigan.transcode.Transcoder;
import com.vaguehope.morrigan.util.DaemonThreadFactory;
import com.vaguehope.morrigan.util.MnLogger;
import com.vaguehope.morrigan.util.StringHelper;

public abstract class AbstractPlayer implements Player {

	private static final MnLogger LOG = MnLogger.make(AbstractPlayer.class);

	private final String id;
	private final String name;
	private final Register<Player> register;
	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final ExecutorService loadEs;
	private final PlayerQueue queue = new DefaultPlayerQueue();
	private final PlayerEventListenerCaller listeners = new PlayerEventListenerCaller();

	private final OrderResolver orderHelper = new OrderResolver();
	private final AtomicReference<PlaybackOrder> playbackOrder = new AtomicReference<PlaybackOrder>(PlaybackOrder.MANUAL);

	private final AtomicReference<Transcode> transcode = new AtomicReference<Transcode>(Transcode.NONE);
	private final Transcoder transcoder = new Transcoder();

	private volatile boolean stateRestoreAttempted = false;
	private volatile boolean loadingTrack = false;

	public AbstractPlayer (final String id, final String name, final PlayerRegister register) {
		this.id = id;
		this.name = name;
		this.register = register;
		this.loadEs = new ThreadPoolExecutor(0, 1, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new DaemonThreadFactory("pload"));
	}

	@Override
	public final void dispose () {
		if (this.alive.compareAndSet(true, false)) {
			try {
				this.register.unregister(this);
			}
			finally {
				saveState();
				onDispose();
				this.loadEs.shutdown();
				this.transcoder.dispose();
			}
		}
	}

	/**
	 * Will be called only once.
	 */
	protected abstract void onDispose ();

	@Override
	public boolean isDisposed () {
		return !this.alive.get();
	}

	protected void checkAlive () {
		if (!this.alive.get()) throw new IllegalStateException("Player is disposed: " + toString());
	}

	protected void markStateRestoreAttempted () {
		this.stateRestoreAttempted = true;
	}

	/**
	 * A hint. May be handled asynchronously.
	 */
	protected void saveState () {
		if (this.stateRestoreAttempted) {
			PlayerStateStorage.writeState(this);
		}
		else {
			LOG.w("Not saving player state as a restore has not yet been attempted.");
		}
	}

	public PlayerEventListener getListeners () {
		return this.listeners;
	}

	@Override
	public String getId () {
		return this.id;
	}

	@Override
	public String getName () {
		return this.name;
	}

	@Override
	public String toString () {
		return new StringBuilder(getClass().getSimpleName()).append("{")
				.append("id=").append(getId())
				.append(" name=").append(getName())
				.append(" order=").append(getPlaybackOrder())
				.append("}").toString();
	}

	@Override
	public void addEventListener (final PlayerEventListener listener) {
		this.listeners.addEventListener(listener);
	}

	@Override
	public void removeEventListener (final PlayerEventListener listener) {
		this.listeners.removeEventListener(listener);
	}

	@Override
	public final int getCurrentTrackDuration () {
		final PlayItem item = getCurrentItem();
		final IMediaTrack track = item != null && item.hasTrack() ? item.getTrack() : null;
		final int trackDuration = track != null ? track.getDuration() : -1;
		return trackDuration > 0 ? trackDuration : getCurrentTrackDurationFromRenderer();
	}

	@Override
	public PlayerQueue getQueue () {
		return this.queue;
	}

	@Override
	public void setPlaybackOrder (final PlaybackOrder order) {
		if (order == null) throw new IllegalArgumentException("Order can not be null.");
		final PlaybackOrder old = this.playbackOrder.getAndSet(order);
		this.listeners.playOrderChanged(order);
		if (order != old) saveState();
	}

	protected OrderResolver getOrderResolver () {
		return this.orderHelper;
	}

	@Override
	public PlaybackOrder getPlaybackOrder () {
		return this.playbackOrder.get();
	}

	@Override
	public void setTranscode (final Transcode transcode) {
		if (transcode == null) throw new IllegalArgumentException("Transcode can not be null.");
		final Transcode old = this.transcode.getAndSet(transcode);
		this.listeners.transcodeChanged(transcode);
		if (transcode != old) saveState();
	}

	@Override
	public Transcode getTranscode () {
		return this.transcode.get();
	}

	@Override
	public final void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list) {
		loadAndStartPlaying(list, null);
	}

	@Override
	public final void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack track) {
		loadAndStartPlaying(new PlayItem(list, track));
	}

	@Override
	public final void loadAndStartPlaying (final PlayItem item) {
		checkAlive();
		if (item == null) throw new IllegalArgumentException("PlayItem can not be null.");

		if (item.getType().isPseudo()) {
			switch (item.getType()) {
				case STOP:
					stopPlaying();
					return;
				default:
					throw new IllegalArgumentException("Do not know how to handle meta type: " + item.getType());
			}
		}

		PlayItem pi = item;
		if (!pi.hasTrack()) {
			// TODO FIXME what if Playback Order is MANUAL?
			final IMediaTrack track = this.orderHelper.getNextTrack(pi.getList(), null, getPlaybackOrder());
			if (track == null) {
				System.err.println("Failed to fill in track: " + pi);
				return;
			}
			pi = pi.withTrack(track);
		}

		loadAndStartPlayingTrack(pi);
	}

	@Override
	public final PlayState getPlayState () {
		if (this.loadingTrack) return PlayState.LOADING;
		return getEnginePlayState();
	}

	public abstract PlayState getEnginePlayState ();

	private void markLoadingState (final boolean isLoading) {
		this.loadingTrack = isLoading;
		getListeners().playStateChanged(getPlayState());
	}

	private final Future<?> loadAndStartPlayingTrack (final PlayItem item) {
		if (item == null) throw new IllegalArgumentException("PlayItem can not be null.");
		if (!item.hasTrack()) throw new IllegalArgumentException("Item must have a track.");
		if (!item.getTrack().isPlayable()) throw new IllegalArgumentException("Item is not playable: '" + item.getTrack().getFilepath() + "'.");

		try {
			if (StringHelper.notBlank(item.getTrack().getFilepath())) {
				final File file = new File(item.getTrack().getFilepath());
				if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
			}
			else if (StringHelper.blank(item.getTrack().getRemoteLocation())) {
				throw new FileNotFoundException("Track has no filepath or remote location: " + item.getTrack());
			}

			markLoadingState(true);
		}
		catch (final Exception e) { // NOSONAR reporting exceptions.
			rollbackToTopOfQueue(item);
			this.listeners.onException(e);
		}

		return this.loadEs.submit(new Runnable() {
			@Override
			public void run () {
				try {
					markLoadingState(true);
					try {
						AbstractPlayer.this.listeners.currentItemChanged(item);

						File altFile = null;
						final TranscodeProfile tProfile = getTranscode().profileForItem(item.getList(), item.getTrack());
						if (tProfile != null) {
							AbstractPlayer.this.transcoder.transcodeToFile(tProfile);
							altFile = tProfile.getCacheFile();
						}

						loadAndPlay(item, altFile);
					}
					finally {
						markLoadingState(false);
					}
				}
				catch (final Exception e) { // NOSONAR reporting exceptions.
					rollbackToTopOfQueue(item);
					AbstractPlayer.this.listeners.onException(e);
				}
			}
		});
	}

	private void rollbackToTopOfQueue (final PlayItem item) {
		if (item.equals(getCurrentItem())) return;
		getQueue().addToQueueTop(item);
	}

	/**
	 * Clients must not call this method.
	 * Already synchronised.
	 * PlayItem has been validated.
	 */
	protected abstract void loadAndPlay (PlayItem item, File altFile) throws Exception;

}
