package com.vaguehope.morrigan.player;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.Register;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.transcode.Transcode;
import com.vaguehope.morrigan.player.transcode.TranscodeProfile;
import com.vaguehope.morrigan.player.transcode.Transcoder;
import com.vaguehope.morrigan.util.Listener;
import com.vaguehope.morrigan.util.StringHelper;

public abstract class AbstractPlayer implements Player {

	private final String id;
	private final String name;
	private final Register<Player> register;
	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final Object[] loadLock = new Object[] {};
	private final PlayerQueue queue = new DefaultPlayerQueue();
	private final PlayerEventListenerCaller listeners = new PlayerEventListenerCaller();
	private final AtomicReference<PlaybackOrder> playbackOrder = new AtomicReference<PlaybackOrder>(PlaybackOrder.MANUAL);
	private final AtomicReference<Transcode> transcode = new AtomicReference<Transcode>(Transcode.NONE);
	private final Transcoder transcoder = new Transcoder();
	private volatile boolean loadingTrack = false;

	public AbstractPlayer (final String id, final String name, final PlayerRegister register) {
		this.id = id;
		this.name = name;
		this.register = register;
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

	/**
	 * A hint.  May be handled asynchronously.
	 */
	protected void saveState () {
		PlayerStateStorage.writeState(this);
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
	public PlayerQueue getQueue () {
		return this.queue;
	}

	@Override
	public void setPlaybackOrder (final PlaybackOrder order) {
		this.playbackOrder.set(order);
		this.listeners.playOrderChanged(order);
	}

	@Override
	public PlaybackOrder getPlaybackOrder () {
		return this.playbackOrder.get();
	}

	@Override
	public void setTranscode (final Transcode transcode) {
		if (transcode == null) throw new IllegalArgumentException("Transcode can not be null.");
		this.transcode.set(transcode);
		this.listeners.transcodeChanged(transcode);
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
			final IMediaTrack track = OrderHelper.getNextTrack(pi.getList(), null, getPlaybackOrder());
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

	private void markLoadingState(final boolean isLoading) {
		this.loadingTrack = isLoading;
		getListeners().playStateChanged(getPlayState());
	}

	private final void loadAndStartPlayingTrack (final PlayItem item) {
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

			synchronized (this.loadLock) {
				markLoadingState(true);
				this.listeners.currentItemChanged(item);

				final TranscodeProfile tProfile = getTranscode().profileForItem(item.getTrack());
				if (tProfile != null) {
					this.transcoder.transcodeToFileAsync(tProfile, new Listener<Exception>() {
						@Override
						public void onAnswer (final Exception transcodeEx) {
							if (transcodeEx == null) {
								try {
									loadAndPlay(item, tProfile.getCacheFile());
								}
								catch (Exception e) {
									AbstractPlayer.this.listeners.onException(e);
								}
								finally {
									markLoadingState(false);
								}
							}
							else {
								AbstractPlayer.this.listeners.onException(transcodeEx);
								markLoadingState(false);
							}
						}
					});
				}
				else {
					try {
						loadAndPlay(item, null);
					}
					finally {
						markLoadingState(false);
					}
				}
			}
		}
		catch (final Exception e) { // NOSONAR reporting exceptions.
			this.listeners.onException(e);
		}
	}

	/**
	 * Clients must not call this method.
	 * Already synchronised.
	 * PlayItem has been validated.
	 */
	protected abstract void loadAndPlay (PlayItem item, File altFile) throws Exception;

}
