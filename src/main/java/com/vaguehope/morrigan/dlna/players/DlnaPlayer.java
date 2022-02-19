package com.vaguehope.morrigan.dlna.players;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.RemoteService;
import org.seamless.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.dlna.content.MediaFileLocator;
import com.vaguehope.morrigan.dlna.httpserver.MediaServer;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.util.Objs;

public class DlnaPlayer extends AbstractDlnaPlayer {

	private static final Logger LOG = LoggerFactory.getLogger(DlnaPlayer.class);

	private final AtomicReference<String> currentUri = new AtomicReference<>();
	private final AtomicReference<WatcherTask> watcher = new AtomicReference<>(null);

	public DlnaPlayer (
			final PlayerRegister register,
			final ControlPoint controlPoint, final RemoteService avTransportSvc,
			final MediaServer mediaServer,
			final MediaFileLocator mediaFileLocator,
			final ScheduledExecutorService scheduledExecutor) {
		super(register, controlPoint, avTransportSvc, mediaServer, mediaFileLocator, scheduledExecutor, null, null);
	}

	@Override
	protected void onDispose () {
		final WatcherTask w = this.watcher.getAndSet(null);
		if (w != null) w.cancel();

		super.onDispose();
	}

	@Override
	protected void dlnaPlay (final PlayItem item, final String id, final String uri, final MimeType mimeType, final long fileSize, final int durationSeconds, final String coverArtUri) throws DlnaException {
		LOG.info("loading: {}", id);
		stopPlaying();

		// Set these fist so if something goes wrong user can try again.
		this.currentUri.set(uri);
		setCurrentItem(item);

		this.avTransport.setUri(id, uri, item.getTrack().getTitle(), mimeType, fileSize, coverArtUri, durationSeconds);
		this.avTransport.play();

		startWatcher(uri, item);
		saveState();

		// Only restore position if for same item.
		final PlayerState rps = getRestorePositionState();
		if (rps != null && rps.getCurrentItem() != null && rps.getCurrentItem().hasTrack()) {
			if (Objs.equals(item.getTrack(), rps.getCurrentItem().getTrack())) {
				final WatcherTask w = this.watcher.get();
				if (w != null) {
					w.requestSeekAfterPlaybackStarts(rps.getPosition());
					LOG.info("Scheduled restore of position: {}s", rps.getPosition());
				}
			}
			else {
				LOG.info("Not restoring position for {} as track is {}.",
						rps.getCurrentItem().getTrack(), item.getTrack());
			}
		}
		clearRestorePositionState();
	}

	private void startWatcher (final String uri, final PlayItem item) {
		final WatcherTask oldWatcher = this.watcher.getAndSet(null);
		if (oldWatcher != null) oldWatcher.cancel();

		final WatcherTask task = WatcherTask.schedule(this.scheduledExecutor,
				uri, this.currentUri,
				item.getTrack().getDuration(),
				this.avTransport,
				getListeners(),
				new OnTrackStarted(this, item), new OnTrackComplete(this, item));
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
				if (ci != null) loadAndStartPlaying(ci);
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
		}
		catch (final DlnaException e) {
			getListeners().onException(e);
		}
	}

}
