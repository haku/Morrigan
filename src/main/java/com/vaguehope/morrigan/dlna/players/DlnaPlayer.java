package com.vaguehope.morrigan.dlna.players;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.meta.RemoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.dlna.players.DlnaPlayingParamsFactory.DlnaPlayingParams;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.PlayerStateStorage;

public class DlnaPlayer extends AbstractDlnaPlayer {

	private static final Logger LOG = LoggerFactory.getLogger(DlnaPlayer.class);

	private final AtomicReference<String> currentUri = new AtomicReference<>();
	private final AtomicReference<WatcherTask> watcher = new AtomicReference<>(null);

	public DlnaPlayer (
			final PlayerRegister register,
			final ControlPoint controlPoint,
			final RemoteService avTransportSvc,
			final DlnaPlayingParamsFactory dlnaPlayingParamsFactory,
			final ScheduledExecutorService scheduledExecutor,
			final PlayerStateStorage playerStateStorage,
			final MediaFactory mediaFactory,
			final Config config) {
		super(register, controlPoint, avTransportSvc, dlnaPlayingParamsFactory, scheduledExecutor, playerStateStorage, mediaFactory, config, null, null);
	}

	@Override
	protected void onDispose () {
		final WatcherTask w = this.watcher.getAndSet(null);
		if (w != null) w.cancel();

		super.onDispose();
	}

	@Override
	protected void dlnaPlay (final PlayItem item, final DlnaPlayingParams playingParams) throws DlnaException {
		LOG.info("loading: {}", playingParams.id);
		stopPlaying();

		// Set these fist so if something goes wrong user can try again.
		this.currentUri.set(playingParams.uri);
		setCurrentItem(item);
		setCurrentPlayingParams(playingParams);

		this.avTransport.setUri(playingParams.id, playingParams.uri, playingParams.title, playingParams.mimeType, playingParams.fileSize, playingParams.coverArtUri, playingParams.durationSeconds);
		this.avTransport.play();

		startWatcher(playingParams.uri, item);
		saveState();

		// Only restore position if for same item.
		final PlayerState rps = getRestorePositionState();
		if (rps != null && rps.getCurrentItem() != null && rps.getCurrentItem().hasItem()) {
			if (Objects.equals(item.getItem(), rps.getCurrentItem().getItem())) {
				final WatcherTask w = this.watcher.get();
				if (w != null) {
					w.requestSeekAfterPlaybackStarts(rps.getPosition());
					LOG.info("Scheduled restore of position: {}s", rps.getPosition());
				}
			}
			else {
				LOG.info("Not restoring position for {} as track is {}.",
						rps.getCurrentItem().getItem(), item.getItem());
			}
		}
		clearRestorePositionState();
	}

	private void startWatcher (final String uri, final PlayItem item) {
		final WatcherTask oldWatcher = this.watcher.getAndSet(null);
		if (oldWatcher != null) oldWatcher.cancel();

		final WatcherTask task = WatcherTask.schedule(this.schEx,
				uri, this.currentUri,
				item.getItem().getDuration(),
				this.avTransport,
				getListeners(),
				this.playbackRecorder,
				item);
		if (!this.watcher.compareAndSet(null, task)) {
			task.cancel();
			LOG.info("Failed to configure watcher as another got there first.");
		}
	}

	@Override
	public void pausePlaying () {
		checkAlive();
		try {
			final PlayState playState = getPlayState();
			if (playState == PlayState.PAUSED) {
				this.avTransport.play();
			}
			else if (playState == PlayState.PLAYING || playState == PlayState.LOADING) {
				this.avTransport.pause();
			}
			else if (playState == PlayState.STOPPED) {
				final PlayItem ci = getCurrentItem();
				if (ci != null) loadAndStartPlaying(ci, 0L, false);
			}
			else {
				LOG.warn("Asked to pause when state is {}, do not know what to do.", playState);
			}
		}
		catch (final DlnaException e) {
			getListeners().onException(e);
		}
	}

	@Override
	public void stopPlaying () {
		checkAlive();
		try {
			this.avTransport.stop();
			getListeners().playStateChanged(PlayState.STOPPED);
		}
		catch (final DlnaException e) {
			getListeners().onException(e);
		}
	}

	@Override
	public PlayState getEnginePlayState () {
		final PlayState ps = this.playerEventCache.getPlayState();
		if (ps != null) return ps;
		return PlayState.STOPPED;
	}

	@Override
	protected boolean shouldBePlaying () {
		PlayState ps = getEnginePlayState();
		return ps == PlayState.PLAYING || ps == PlayState.LOADING;
	}

	@Override
	public void seekTo (final double d) {
		checkAlive();
		try {
			this.avTransport.seek((long) (getCurrentTrackDuration() * d));
			getListeners().afterSeek();
		}
		catch (final DlnaException e) {
			getListeners().onException(e);
		}
	}

	@Override
	public void setPositionMillis(long millis) {
		checkAlive();
		try {
			this.avTransport.seek(TimeUnit.MILLISECONDS.toSeconds(millis));
			getListeners().afterSeek();
		}
		catch (final DlnaException e) {
			getListeners().onException(e);
		}
	}

	@Override
	public Integer getVolume () {
		return null;
	}

	@Override
	public Integer getVolumeMaxValue () {
		return null;
	}

	@Override
	public void setVolume (final int newVolume) {
		throw new UnsupportedOperationException("setVolume not implemented.");
	}

}
