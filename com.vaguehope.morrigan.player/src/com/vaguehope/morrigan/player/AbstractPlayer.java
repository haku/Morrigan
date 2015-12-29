package com.vaguehope.morrigan.player;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.vaguehope.morrigan.model.Register;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;

public abstract class AbstractPlayer implements Player {

	private final String id;
	private final String name;
	private final Register<Player> register;
	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final Object[] loadLock = new Object[] {};
	private final PlayerQueue queue = new DefaultPlayerQueue();
	private final PlayerEventListenerCaller listeners = new PlayerEventListenerCaller();
	private final AtomicReference<PlaybackOrder> playbackOrder = new AtomicReference<PlaybackOrder>(PlaybackOrder.SEQUENTIAL);

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
				onDispose();
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
	public void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list) {
		loadAndStartPlaying(list, null);
	}

	@Override
	public void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack track) {
		loadAndStartPlaying(new PlayItem(list, track));
	}

	@Override
	public void loadAndStartPlaying (final PlayItem item) {
		checkAlive();
		if (item == null) throw new IllegalArgumentException("PlayItem can not be null.");

		PlayItem pi = item;
		if (!pi.hasTrack()) {
			final IMediaTrack track = OrderHelper.getNextTrack(pi.getList(), null, getPlaybackOrder());
			if (track == null) return;
			pi = pi.withTrack(track);
		}

		loadAndStartPlayingTrack(pi);
	}

	private void loadAndStartPlayingTrack (final PlayItem item) {
		if (item == null) throw new IllegalArgumentException("PlayItem can not be null.");
		if (!item.hasTrack()) throw new IllegalArgumentException("Item must have a track.");
		if (!item.getTrack().isPlayable()) throw new IllegalArgumentException("Item is not playable: '" + item.getTrack().getFilepath() + "'.");
		try {
			final File file = new File(item.getTrack().getFilepath());
			if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
			synchronized (this.loadLock) {
				loadAndStartPlaying(item, file);
				this.listeners.currentItemChanged(item);
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
	protected abstract void loadAndStartPlaying (PlayItem item, File file) throws Exception;

}
