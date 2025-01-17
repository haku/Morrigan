package com.vaguehope.morrigan.dlna.players;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jupnp.support.model.MediaInfo;
import org.jupnp.support.model.PositionInfo;
import org.jupnp.support.model.TransportInfo;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.model.TransportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlaybackRecorder;
import com.vaguehope.morrigan.player.Player.PlayerEventListener;
import com.vaguehope.morrigan.util.ErrorHelper;

final class WatcherTask implements Runnable {

	private static final int COUNTS_AS_STARTED_SECONDS = 5;
	private static final Logger LOG = LoggerFactory.getLogger(WatcherTask.class);

	public static WatcherTask schedule (
			final ScheduledExecutorService scheduledExecutor,
			final String uriToWatch,
			final AtomicReference<String> currentUri,
			final int trackDurationSeconds,
			final AvTransportActions avTransport,
			final PlayerEventListener listener,
			final PlaybackRecorder playbackRecorder,
			final PlayItem item
			) {
		final WatcherTask task = new WatcherTask(uriToWatch, currentUri, trackDurationSeconds, avTransport, listener, playbackRecorder, item);
		final ScheduledFuture<?> scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(task, 1, 1, TimeUnit.SECONDS);
		task.setFuture(scheduledFuture);
		return task;
	}

	private final String uriToWatch;
	private final AtomicReference<String> currentUri;
	private final int trackDurationSeconds;
	private final AvTransportActions avTransport;
	private final PlayerEventListener listener;
	private final PlaybackRecorder playbackRecorder;
	private final PlayItem item;

	private final AtomicReference<ScheduledFuture<?>> scheduledFuture = new AtomicReference<>();
	private volatile long restorePositionAfterPlaybackStarts;

	private final AtomicBoolean trackStarted = new AtomicBoolean(false);
	private final AtomicBoolean trackEnded = new AtomicBoolean(false);
	private volatile long startTime = 0L;

	private volatile long lastElapsedSeconds = -1;
	private volatile long lastDurationSeconds = -1;

	private WatcherTask (final String uriToWatch, final AtomicReference<String> currentUri,
			final int trackDurationSeconds,
			final AvTransportActions avTransport,
			final PlayerEventListener listener, final PlaybackRecorder playbackRecorder, final PlayItem item) {
		this.uriToWatch = uriToWatch;
		this.currentUri = currentUri;
		this.trackDurationSeconds = trackDurationSeconds;
		this.avTransport = avTransport;
		this.listener = listener;
		this.playbackRecorder = playbackRecorder;
		this.item = item;
	}

	public void requestSeekAfterPlaybackStarts (final long position) {
		this.restorePositionAfterPlaybackStarts = position;
	}

	public void cancel () {
		final ScheduledFuture<?> sf = this.scheduledFuture.getAndSet(null);
		if (sf != null) sf.cancel(false);
		callEndOfTrack(false);
	}

	@Override
	public void run () {
		try {
			runOrThrow();
		}
		catch (final DlnaException e) {
			LOG.warn("DLNA error: {}", ErrorHelper.oneLineCauseTrace(e));
		}
		catch (final Exception e) {
			LOG.warn("Unhandled exception in watcher.", e);
		}
	}

	public void runOrThrow () throws DlnaException {
		if (this.trackEnded.get()) {
			cancel();
			LOG.info("Watcher cancelled; track ended.");
			return;
		}

		final String uri = this.currentUri.get();
		if (!this.uriToWatch.equals(uri)) { // Player is playing a different track.
			cancel();
			LOG.info("Watcher cancelled; player's currentUri changed to: {}.", uri);
			return;
		}

		// If we already know the duration, prefer that to trusting the remote device.
		final long durSecs = this.trackDurationSeconds > 1
				? this.trackDurationSeconds
				: this.lastDurationSeconds;

		// Basically, does it look like its been playing OK for a bit?
		final boolean probabblyBeenPlayingOk =
				durSecs > 0
				? (this.lastElapsedSeconds / (double) durSecs) > 0.9
				: this.lastElapsedSeconds > 30;

		final MediaInfo mi = this.avTransport.getMediaInfo();
		final String remoteUri = mi != null ? mi.getCurrentURI() : null;
		if (remoteUri == null && probabblyBeenPlayingOk) {
			LOG.info("Probably finished: " + uri);
			callEndOfTrack(true);
			cancel();
			return;
		}
		// Cancelled?  Rendered failed during playback or decided to do something else.
		if (!uri.equals(remoteUri)) { // Renderer is playing a different track.
			this.listener.currentItemChanged(null); // TODO parse currentURIMetadata and create mock item with track title?
			cancel();
			LOG.info("Watcher cancelled; renderer's currentUri changed to: {}.", remoteUri);
			this.listener.playStateChanged(PlayState.STOPPED);
			return;
		}

		final TransportInfo ti = this.avTransport.getTransportInfo();
		this.listener.playStateChanged(AbstractDlnaPlayer.transportIntoToPlayState(ti));
		if (ti == null) {
			cancel();
			LOG.info("Watcher cancelled; renderer returned null transport info.");
			return;
		}

		if (ti.getCurrentTransportStatus() != TransportStatus.OK) {
			LOG.warn("Current transport status: {}", ti.getCurrentTransportStatus());
			return;
		}

		if (ti.getCurrentTransportState() == TransportState.STOPPED
				|| ti.getCurrentTransportState() == TransportState.NO_MEDIA_PRESENT) {
			if (probabblyBeenPlayingOk) {
				LOG.info("Finished: " + uri);
				callEndOfTrack(true);
			}
			cancel();
		}

		final PositionInfo pi = this.avTransport.getPositionInfo();
		if (pi == null) {
			this.listener.positionChanged(-1, -1);
		}
		else {
			final long elapsedSeconds = pi.getTrackElapsedSeconds();
			final long durationSeconds = pi.getTrackDurationSeconds();

			if (elapsedSeconds > 0) this.lastElapsedSeconds = elapsedSeconds;
			if (durationSeconds > 0) this.lastDurationSeconds = durationSeconds;

			this.listener.positionChanged(elapsedSeconds, (int) durationSeconds);
			if (!this.trackStarted.get() && elapsedSeconds > COUNTS_AS_STARTED_SECONDS) callStartOfTrack();

			if (elapsedSeconds > 0) {
				final long posToRestore = this.restorePositionAfterPlaybackStarts;
				if (posToRestore > 0) {
					this.avTransport.seek(posToRestore);
					LOG.info("Restored position: {}s", posToRestore);
					this.restorePositionAfterPlaybackStarts = 0;
				}
			}
		}
	}

	private void callStartOfTrack () {
		if (this.trackStarted.compareAndSet(false, true)) {
			this.startTime = System.currentTimeMillis();
			this.playbackRecorder.recordStarted(this.item);
		}
	}

	private void callEndOfTrack (final boolean completed) {
		if (!this.trackStarted.get()) return;

		if (this.trackEnded.compareAndSet(false, true)) {
			this.playbackRecorder.recordCompleted(this.item, completed, this.startTime);
		}
	}

	private void setFuture (final ScheduledFuture<?> scheduledFuture) {
		if (!this.scheduledFuture.compareAndSet(null, scheduledFuture)) {
			throw new IllegalStateException("ScheduledFuture already set.");
		}
	}

}
