package com.vaguehope.morrigan.player;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.transcode.Transcode;
import com.vaguehope.morrigan.transcode.TranscodeContext;
import com.vaguehope.morrigan.transcode.TranscodeProfile;
import com.vaguehope.morrigan.transcode.Transcoder;
import com.vaguehope.morrigan.util.StringHelper;

public abstract class AbstractPlayer implements Player {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractPlayer.class);

	protected final ScheduledExecutorService schEx;
	protected final PlaybackRecorder playbackRecorder;

	private final String id;
	private final String name;
	private final PlayerRegister register;
	private final PlayerStateStorage playerStateStorage;
	private final Config config;

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final List<Runnable> onDisposeListener = new ArrayList<>();

	private final PlayerQueue queue = new DefaultPlayerQueue();
	private final PlayerEventListenerCaller listeners = new PlayerEventListenerCaller();

	private final AtomicReference<PlaybackOrder> playbackOrder = new AtomicReference<>(PlaybackOrder.MANUAL);
	private volatile PlaybackOrder playbackOrderOverride = null;

	private final AtomicReference<MediaList> currentList = new AtomicReference<>();
	private final AtomicReference<PlayItem> currentItem = new AtomicReference<>();
	private final AtomicInteger currentItemDurationSeconds = new AtomicInteger(-1);

	private final AtomicReference<Transcode> transcode = new AtomicReference<>(Transcode.NONE);
	private final Transcoder transcoder;

	private volatile boolean stateRestoreAttempted = false;
	private volatile boolean loadingTrack = false;

	public AbstractPlayer(
			final String id,
			final String name,
			final PlayerRegister register,
			final ScheduledExecutorService schEx,
			final PlayerStateStorage playerStateStorage,
			final Config config) {
		this.id = id;
		this.name = name;
		this.register = register;
		this.schEx = schEx;
		this.playerStateStorage = playerStateStorage;
		this.config = config;
		this.playbackRecorder = new PlaybackRecorder(schEx);
		this.transcoder = new Transcoder(id);
	}

	@Override
	public final void dispose () {
		if (this.alive.compareAndSet(true, false)) {
			try {
				this.register.unregister(this);
			}
			finally {
				try {
					for (final Runnable runnable : this.onDisposeListener) {
						runnable.run();
					}
				}
				finally {
					saveState();
					this.transcoder.dispose();
					onDispose();
				}
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

	@Override
	public void addOnDisposeListener(final Runnable runnable) {
		this.onDisposeListener.add(runnable);
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
			this.playerStateStorage.writeState(this);
		}
		else {
			LOG.info("Not saving player state as a restore has not yet been attempted.");
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
	public MediaList getCurrentList() {
		MediaList list = this.currentList.get();
		if (list != null) return list;

		final PlayItem item = getCurrentItem();
		list = item == null ? null : item.getList();
		if (list != null) this.currentList.compareAndExchange(null, list);

		return this.currentList.get();
	}

	@Override
	public void setCurrentItem(final PlayItem item) {
		final PlayItem old = this.currentItem.getAndSet(item);
		if (!Objects.equals(old, item)) {
			this.currentItemDurationSeconds.set(-1);
		}
		getListeners().currentItemChanged(item);
	}

	@Override
	public PlayItem getCurrentItem() {
		return this.currentItem.get();
	}

	@Override
	public int getCurrentTrackDurationAsMeasured() {
		return this.currentItemDurationSeconds.get();
	}

	protected void setCurrentTrackDurationAsMeasured(final int newDuration) {
		this.currentItemDurationSeconds.set(newDuration);
	}

	@Override
	public final int getCurrentTrackDuration () {
		final int asMeasured = getCurrentTrackDurationAsMeasured();
		if (asMeasured > 0) return asMeasured;

		final PlayItem item = getCurrentItem();
		final MediaItem track = item != null && item.hasTrack() ? item.getTrack() : null;
		final int trackDuration = track != null ? track.getDuration() : -1;
		if (trackDuration > 0) return trackDuration;

		return getCurrentTrackDurationFromRenderer();
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

	@Override
	public PlaybackOrder getPlaybackOrder () {
		return this.playbackOrder.get();
	}

	@Override
	public PlaybackOrder getPlaybackOrderOverride() {
		return this.playbackOrderOverride;
	}

	@Override
	public Integer getVoume () {
		return null;
	}

	@Override
	public Integer getVoumeMaxValue () {
		return null;
	}

	@Override
	public void setVolume (final int newVolume) {
		throw new UnsupportedOperationException("setVolume not implemented.");
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
	public final void loadAndStartPlaying (final MediaList list) {
		loadAndStartPlaying(list, null);
	}

	@Override
	public final void loadAndStartPlaying (final MediaList list, final MediaItem track) {
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
			if (!pi.hasList()) return;  // can not pick a track without a list.

			this.currentList.set(pi.getList());  // list only items update the current list.

			final MediaItem track = findTrackForListOnlyPlayItem(pi);
			if (track == null) return;
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
			else if (!item.getTrack().hasRemoteLocation()) {
				throw new FileNotFoundException("Track has no filepath or remote location: " + item.getTrack());
			}

			markLoadingState(true);
		}
		catch (final Exception e) { // NOSONAR reporting exceptions.
			rollbackToTopOfQueue(item);
			this.listeners.onException(e);
		}

		return this.schEx.submit(new Runnable() {
			@Override
			public void run () {
				try {
					markLoadingState(true);
					try {
						AbstractPlayer.this.listeners.currentItemChanged(item);

						PlayItem maybeUpdatedItem = item;

						final TranscodeProfile tProfile = getTranscode().profileForItem(new TranscodeContext(AbstractPlayer.this.config), item.getList(), item.getTrack());
						if (tProfile != null) {
							AbstractPlayer.this.transcoder.transcodeToFile(tProfile);
							final File altFile = tProfile.getCacheFileEvenIfItDoesNotExist();  // This should exist because the transcode just ran.
							maybeUpdatedItem = item.withAltFile(altFile);
						}

						loadAndPlay(maybeUpdatedItem);
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
		this.queue.addToQueueTop(item.withoutId());
	}

	@Override
	public void nextTrack () {
		checkAlive();
		try {
			final PlayItem nextItemToPlay = findNextItemToPlay();
			if (nextItemToPlay != null) {
				loadAndStartPlaying(nextItemToPlay);
			}
			else {
				stopPlaying();
			}
		}
		catch (final MorriganException e) {
			getListeners().onException(e);
		}
	}

	private MediaItem findTrackForListOnlyPlayItem(final PlayItem pi) {
		PlaybackOrder order = getPlaybackOrder();
		if (order == PlaybackOrder.MANUAL && pi.hasId()) {
			// For a queue item we need to find a track, even if in MANUAL mode.
			// And since there is no UI to specify anything else, default to RANDOM.
			order = PlaybackOrder.RANDOM;
		}
		try {
			return callChooseItemOnList(pi.getList(), order, null);
		}
		catch (final MorriganException e) {
			this.listeners.onException(e);
			return null;
		}
	}

	/**
	 * Clients must not call this method.
	 * Already synchronised.
	 * PlayItem has been validated.
	 */
	protected abstract void loadAndPlay (PlayItem item) throws Exception;

	protected PlayItem findNextItemToPlay () throws MorriganException {
		final PlayItem queueItem = this.queue.takeFromQueue();
		if (queueItem != null) return queueItem;

		final PlayItem current = getCurrentItem();
		final MediaList curList = getCurrentList();
		final PlaybackOrder pbOrder = getPlaybackOrder();

		if (current != null && current.isComplete() && current.getList().equals(curList)) {
			final MediaItem nextTrack = callChooseItemOnList(current.getList(), pbOrder, current.getTrack());
			if (nextTrack != null) {
				return new PlayItem(current.getList(), nextTrack);
			}
		}

		final MediaItem nextTrack = callChooseItemOnList(curList, pbOrder, null);
		if (nextTrack != null) {
			return new PlayItem(curList, nextTrack);
		}

		return null;
	}

	private MediaItem callChooseItemOnList(final MediaList list, final PlaybackOrder order, MediaItem item) throws MorriganException {
		if (list == null) return null;
		if (order == PlaybackOrder.MANUAL) return null;  // do this here so list impls do not have to.

		final PlaybackOrder orderToUse = list.getSupportedChooseMethods().contains(order) ? order : list.getDefaultChooseMethod();
		this.playbackOrderOverride = orderToUse != order ? orderToUse : null;

		final MediaItem ret = list.chooseItem(orderToUse, item);
		LOG.info("list {} choose using {}: {}", list, orderToUse, ret);
		return ret;
	}

}
