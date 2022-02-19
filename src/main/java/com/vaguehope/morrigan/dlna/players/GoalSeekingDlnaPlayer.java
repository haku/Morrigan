package com.vaguehope.morrigan.dlna.players;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportState;
import org.seamless.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.dlna.DlnaResponseException;
import com.vaguehope.morrigan.dlna.content.MediaFileLocator;
import com.vaguehope.morrigan.dlna.httpserver.MediaServer;
import com.vaguehope.morrigan.dlna.util.Quietly;
import com.vaguehope.morrigan.dlna.util.Timestamped;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.util.ErrorHelper;
import com.vaguehope.morrigan.util.Objs;

public class GoalSeekingDlnaPlayer extends AbstractDlnaPlayer {

	private static final int SLOW_RETRIES_AFTER_SECONDS = 60;
	private static final int SLOW_RETRIE_DELAY_SECONDS = 10;

	private static final int MAX_SEQUENTIAL_EMPTY_DLNA_RESPONSES = 10;

	private static final int MIN_POSITION_TO_RECORD_STARTED_SECONDS = 5;
	private static final int MIN_POSITION_TO_RESTORE_SECONDS = 10;

	// At least 98% of file played, or only 5 seconds left, which ever is sooner.
	private static final double END_TOLERANCE_MIN_RATIO = 0.98d;
	private static final int END_TOLERANCE_MIN_SECONDS = 5;

	/**
	 * If there should be something playing but there is not, wait at least this long before trying to play it again.
	 * This allows time for session end event to arrive and be processed.
	 */
	private static final int WAIT_FOR_STOP_EVENT_TIMEOUT_SECONDS = END_TOLERANCE_MIN_SECONDS + 5;

	private static final Logger LOG = LoggerFactory.getLogger(GoalSeekingDlnaPlayer.class);

	private final ScheduledFuture<?> schdFuture;

	public GoalSeekingDlnaPlayer (
			final PlayerRegister register,
			final ControlPoint controlPoint,
			final RemoteService avTransportSvc,
			final MediaServer mediaServer,
			final MediaFileLocator mediaFileLocator,
			final ScheduledExecutorService scheduledExecutor) {
		this(register, controlPoint, avTransportSvc, mediaServer, mediaFileLocator, scheduledExecutor, null, null);
	}

	public GoalSeekingDlnaPlayer (
			final PlayerRegister register,
			final ControlPoint controlPoint,
			final RemoteService avTransportSvc,
			final MediaServer mediaServer,
			final MediaFileLocator mediaFileLocator,
			final ScheduledExecutorService scheduledExecutor,
			final AvTransportActions avTransportActions,
			final RenderingControlActions renderingControlActions) {
		super(register, controlPoint, avTransportSvc, mediaServer, mediaFileLocator, scheduledExecutor, avTransportActions, renderingControlActions);
		controlPoint.execute(new AvSubscriber(this, this.avEventListener, avTransportSvc, 600));
		this.schdFuture = scheduledExecutor.scheduleWithFixedDelay(this.schdRunner, 1, 1, TimeUnit.SECONDS);
	}

	@Override
	protected void onDispose () {
		this.schdFuture.cancel(true);
		super.onDispose();
	}

	private final Runnable schdRunner = new Runnable() {
		@Override
		public void run () {
			runAndDoNotThrow();
		}
	};

	private final AvEventListener avEventListener = new AvEventListener() {
		@Override
		public void onTransportState (final TransportState transportState) {
			GoalSeekingDlnaPlayer.this.eventQueue.add(transportState);
		}
	};

	private volatile long lastSuccessNanos = System.nanoTime();
	private final AtomicInteger sequentialEmptyDlnaResponses = new AtomicInteger(0);
	private final AtomicBoolean selfDestructTriggered = new AtomicBoolean(false);

	private void markLastSuccess () {
		this.lastSuccessNanos = System.nanoTime();
		this.sequentialEmptyDlnaResponses.set(0);
	}

	private long secondsSinceLastSuccess () {
		return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - this.lastSuccessNanos);
	}

	private void markEmptyDlnaResponse () {
		final int n = this.sequentialEmptyDlnaResponses.incrementAndGet();
		if (n >= MAX_SEQUENTIAL_EMPTY_DLNA_RESPONSES) {
			if (this.selfDestructTriggered.compareAndSet(false, true)) {
				LOG.warn("Self destructing, {} sequential empty DLNA responses.", n);
				this.controlPoint.getRegistry().removeDevice(this.avTransportSvc.getDevice());
				this.controlPoint.search();
			}
		}
	}

	private void runAndDoNotThrow () {
		try {
			readEventQueue();
			setAndReadVolume();

			final PlayState cState = readStateAndSeekGoal();
			setStateToReportExternally(cState);

			markLastSuccess();
		}
		catch (final DlnaResponseException e) {
			LOG.warn("DLNA call failed: {}", ErrorHelper.oneLineCauseTrace(e));
			if (e.emptyResponse()) markEmptyDlnaResponse();
		}
		catch (final DlnaException e) {
			LOG.warn("DLNA call failed: {}", ErrorHelper.oneLineCauseTrace(e));
		}
		catch (final Exception e) {
			LOG.warn("Unhandled error in event thread.", e);
		}

		if (secondsSinceLastSuccess() > SLOW_RETRIES_AFTER_SECONDS) {
			Quietly.sleep(SLOW_RETRIE_DELAY_SECONDS, TimeUnit.SECONDS); // Rate limit errors.
		}
	}

	/**
	 * Different from goalState because LOADING is not a valid goal and there may be intermediate steps.
	 */
	private volatile PlayState stateToReportExternally = null;
	private final BlockingQueue<Object> eventQueue = new LinkedBlockingQueue<Object>();
	/*
	 * These fields must only be written from the event thread.
	 */
	private volatile DlnaToPlay goalToPlay = null;
	private volatile boolean rendererNeedsCleaning = false;
	private volatile PlayState goalState = null;
	private volatile Long goalSeekToSeconds = null;
	private volatile Timestamped<Long> lastObservedPositionSeconds = Timestamped.old(0L);
	/**
	 * Specifically goToPlay has finished playing.
	 */
	private volatile boolean unprocessedPlaybackOfGoalStoppedEvent = false;
	private volatile Integer renderVolume = null;
	private volatile Integer goalRenderVolume = null;

	// visible for testing.
	public void readEventQueue () {
		final DlnaToPlay prevToPlay = this.goalToPlay;
		boolean stopEvent = false;

		Object obj;
		while ((obj = this.eventQueue.poll()) != null) {
			if (obj instanceof PlayState) {
				final PlayState newState = (PlayState) obj;
				if (newState == PlayState.LOADING) throw new IllegalStateException("Loading is not a valid target state.");
				this.goalState = newState;
				setStateToReportExternally(newState);
			}
			else if (obj instanceof Integer) {
				this.goalRenderVolume = (Integer) obj;
			}
			else if (obj instanceof Long) {
				this.goalSeekToSeconds = (Long) obj;
				this.lastObservedPositionSeconds = Timestamped.of((Long) obj);
			}
			else if (obj instanceof DlnaToPlay) {
				this.goalToPlay = (DlnaToPlay) obj;
				this.lastObservedPositionSeconds = Timestamped.old(0L);
				// User has requested a new track, but it might be the same as the old track.
				// Which would really confuse things, so this flag is to send an explicit Stop
				// to clean any old state from the renderer.
				this.rendererNeedsCleaning = true;
			}
			else if (obj instanceof TransportState) {
				final TransportState transportState = (TransportState) obj;
				if (transportState == TransportState.STOPPED || transportState == TransportState.NO_MEDIA_PRESENT) {
					stopEvent = true;
				}
			}
			else {
				LOG.warn("Unexpected {} type on event queue: {}", obj.getClass(), obj);
			}
		}

		if (this.goalToPlay != null && this.goalToPlay == prevToPlay && stopEvent) {
			LOG.info("Track finished playing event for: {}", this.goalToPlay.getId());
			this.unprocessedPlaybackOfGoalStoppedEvent = true;
		}
	}

	private void setAndReadVolume() throws DlnaException {
		if (this.renderingControl != null) {
			if (this.goalRenderVolume != null) {
				this.renderingControl.setVolume(this.goalRenderVolume);
			}
			this.renderVolume = this.renderingControl.getVolume();
		}
		this.goalRenderVolume = null;
	}

	private volatile TransportState prevRenState = null;
	private volatile String prevRenUri = null;

	/**
	 * Returns the state that should be shown externally in UIs, etc.
	 * Visible for testing.
	 */
	public PlayState readStateAndSeekGoal () throws DlnaException {
		// If a clean up is needed, everything else might be invalid.
		if (this.rendererNeedsCleaning) {
			LOG.debug("Sending cleanup Stop()...");
			this.avTransport.stop();
			this.rendererNeedsCleaning = false;
		}

		// Capture state.
		final DlnaToPlay goToPlay = this.goalToPlay;
		final PlayState goState = this.goalState;

		// If no goal state, do not do anything.
		if (goToPlay == null) return PlayState.STOPPED;
		if (goState == null) return PlayState.STOPPED;

		// Read renderer state.
		final MediaInfo renMi = this.avTransport.getMediaInfo();
		final TransportInfo renTi = this.avTransport.getTransportInfo();
		final PositionInfo renPi = this.avTransport.getPositionInfo();
		LOG.debug("PositionInfo: {}", renPi);

		// Check playback progress.  This needs to happen before checking state to ensure LOP is as up to date as possible.
		final long renElapsedSeconds;
		final long renDurationSeconds;
		if (renPi == null) {
			renElapsedSeconds = -1;
			renDurationSeconds = -1;
		}
		else {
			renElapsedSeconds = renPi.getTrackElapsedSeconds();
			renDurationSeconds = renPi.getTrackDurationSeconds();
		}

		// Get things ready to compare.
		final TransportState renState = renTi.getCurrentTransportState();
		final String renUri = renMi != null ? renMi.getCurrentURI() : null;

		if (renState != this.prevRenState || !Objs.equals(renUri, this.prevRenUri)) {
			LOG.info("Renderer: {} {}", renState, renUri);
			this.prevRenState = renState;
			this.prevRenUri = renUri;
		}

		// How many seconds of the file MUST be played?
		final long minPlayedSecondsRatio = (long) (goToPlay.getDurationSeconds() * END_TOLERANCE_MIN_RATIO);
		final long minPlayedSecondsOffset = goToPlay.getDurationSeconds() - END_TOLERANCE_MIN_SECONDS;
		final long minPlayedSecondsMin = Math.min(minPlayedSecondsRatio, minPlayedSecondsOffset); // Which ever is easier to achieve.
		final long minPlayedSeconds = Math.max(minPlayedSecondsMin, 1); // Durations less than one second are going to cause issues.

		// Has the track finished playing?
		final long maxSecondsThatHaveBeenPlayed = Math.max(renElapsedSeconds, this.lastObservedPositionSeconds.get());
		final boolean lopAtEnd = maxSecondsThatHaveBeenPlayed >= minPlayedSeconds;
		final boolean trackNotPlaying = renUri == null || renState == TransportState.STOPPED || renState == TransportState.NO_MEDIA_PRESENT;
		final boolean rendererStoppedPlaying = this.unprocessedPlaybackOfGoalStoppedEvent || (trackNotPlaying && lopAtEnd);
		this.unprocessedPlaybackOfGoalStoppedEvent = false;

		// Did the render stop on its own?
		if (rendererStoppedPlaying) {
			LOG.info("Assuming playback stopped: {}", goToPlay.getId());

			// Track ended event.
			if (lopAtEnd) {
				LOG.info("Assuming track was played to end: {} ({}s of {}s)", goToPlay.getId(), maxSecondsThatHaveBeenPlayed, goToPlay.getDurationSeconds());
				this.goalToPlay.recordEndOfTrack();

				// Make the UI show that the end of the track was reached exactly.
				getListeners().positionChanged(goToPlay.getDurationSeconds(), goToPlay.getDurationSeconds());

				// Clear goal state.
				this.goalToPlay = null;
				this.goalSeekToSeconds = null;
				this.lastObservedPositionSeconds = Timestamped.old(0L); // Old skips WAIT_FOR_STOP_EVENT_TIMEOUT_SECONDS delay.

				return PlayState.STOPPED; // Made a change, so return.
			}

			LOG.info("But track did not play to end, going to try again from {}s...", this.lastObservedPositionSeconds);
		}

		// If renderer is between states or a strange state, wait.
		if (renState != null) {
			switch (renState) {
				case CUSTOM:
				case TRANSITIONING:
					LOG.info("Waiting for renderer to leave state: {}", renState);
					return PlayState.LOADING;
				default:
			}
		}

		// Should stop?
		if (goState == PlayState.STOPPED) {
			if (renState != null) {
				switch (renState) {
					case PAUSED_PLAYBACK:
					case PAUSED_RECORDING:
					case PLAYING:
					case RECORDING:
						this.avTransport.stop();
						LOG.info("Stopped.");
						return PlayState.STOPPED; // Made a change, so return.
					default:
				}
			}
			this.goalToPlay = null;
			this.goalSeekToSeconds = null;
			LOG.info("Cleared goal state.");
			return PlayState.STOPPED; // Target state reached.  Stop.
		}

		// Renderer got the right URI?  If not, start playing right URL.
		if (!Objs.equals(renUri, goToPlay.getUri())) {
			if (goState == PlayState.PAUSED) return PlayState.PAUSED; // We would load, but will wait until not paused before doing so.

			// If age of last observed position is too young, wait a bit in case end event turns up.
			if (this.lastObservedPositionSeconds.age(TimeUnit.SECONDS) < WAIT_FOR_STOP_EVENT_TIMEOUT_SECONDS) {
				LOG.debug("Waiting for posible end event...");
				return PlayState.LOADING;
			}

			if (renUri != null) {
				LOG.info("Stopping: {}", renUri);
				try {
					this.avTransport.stop();
				}
				catch (final DlnaResponseException e) { // Specifically not a timeout.
					LOG.info("Stop before play failed: {}", ErrorHelper.oneLineCauseTrace(e));
				}
			}

			LOG.info("Loading: {}", goToPlay);
			this.avTransport.setUri(
					goToPlay.getId(),
					goToPlay.getUri(),
					goToPlay.getItem().getTrack().getTitle(),
					goToPlay.getMimeType(), goToPlay.getFileSize(),
					goToPlay.getCoverArtUri(),
					goToPlay.getDurationSeconds());
			this.avTransport.play();
			LOG.debug("Loaded: {}.", goToPlay.getId());
			scheduleRestorePosition(this.lastObservedPositionSeconds.get());
			return PlayState.LOADING; // Made a change, so return.
		}

		// Should resume / pause?
		if (goState == PlayState.PAUSED) {
			if (renState != null) {
				switch (renState) {
					case PLAYING:
					case RECORDING:
						try {
							this.avTransport.pause();
							LOG.info("Paused.");
						}
						catch (final DlnaResponseException e) {
							if (e.hasStatusCodeBetween(500, 600)) {
								this.avTransport.stop();
								LOG.info("Paused failed, stopped instead.");
							}
							else {
								throw e;
							}
						}
						return PlayState.PAUSED; // Made a change, so return.
					default:
				}
			}
		}
		else if (goState == PlayState.PLAYING) {
			if (renState != null) {
				switch (renState) {
					case STOPPED:
					case NO_MEDIA_PRESENT:
						this.avTransport.play();
						LOG.info("Started playback.");
						scheduleRestorePosition(this.lastObservedPositionSeconds.get());
						return PlayState.PLAYING; // Made a change, so return.

					case PAUSED_PLAYBACK:
					case PAUSED_RECORDING:
						this.avTransport.play();
						LOG.info("Resumed.");
						return PlayState.PLAYING; // Made a change, so return.

					default:
				}
			}
		}

		// Stash current play back progress if greater than progress so far.
		if (renElapsedSeconds > this.lastObservedPositionSeconds.get()) {
			this.lastObservedPositionSeconds = Timestamped.of(renElapsedSeconds);
		}

		// Notify event listeners.
		getListeners().positionChanged(renElapsedSeconds, (int) renDurationSeconds);

		// track started event.  recordStartOfTrack() expects ignore multiple invocations.
		if (renElapsedSeconds > MIN_POSITION_TO_RECORD_STARTED_SECONDS) {
			this.goalToPlay.recordStartOfTrack();
		}

		// External state can now reflect renderer state.
		final PlayState renPlayState = transportIntoToPlayState(renTi);

		// Need to seek to position?
		// Check and set should be safe as only our thread should be updating it.
		if (renElapsedSeconds > 0 && this.goalSeekToSeconds != null && this.goalSeekToSeconds >= 0) {
			this.avTransport.seek(this.goalSeekToSeconds);
			LOG.info("Set position to {}s.", this.goalSeekToSeconds);
			this.goalSeekToSeconds = null;
			return renPlayState; // Made a change, so return.
		}

		return renPlayState;
	}

	private void scheduleRestorePosition (final long lopSeconds) {
		if (lopSeconds > MIN_POSITION_TO_RESTORE_SECONDS) {
			this.eventQueue.add(Long.valueOf(lopSeconds));
			LOG.info("Recovery scheduled restore position: {}s", lopSeconds);
		}
	}

	private void schedulePlayStateChange (final PlayState ps) {
		if (this.eventQueue.add(ps)) {
			setStateToReportExternally(ps);
		}
	}

	private void setStateToReportExternally (final PlayState state) {
		this.stateToReportExternally = state;
		getListeners().playStateChanged(this.stateToReportExternally);
	}

	@Override
	public PlayState getEnginePlayState () {
		final PlayState ps = this.stateToReportExternally;
		if (ps != null) return ps;
		return PlayState.STOPPED;
	}

	@Override
	public Integer getVoume () {
		final Integer goal = this.goalRenderVolume;
		if (goal != null) return goal;
		return this.renderVolume;
	}

	@Override
	public Integer getVoumeMaxValue () {
		if (this.renderingControl == null) return null;
		return this.renderingControl.getVolumeMaxValue();
	}

	@Override
	public void setVolume (final int newVolume) {
		this.eventQueue.add(Integer.valueOf(newVolume));
		this.renderVolume = newVolume;  // hacky but makes the ui more responsive.
	}

	@Override
	protected boolean shouldBePlaying () {
		// goal state can not be LOADING.
		return this.goalState == PlayState.PLAYING;
	}

	@Override
	public void pausePlaying () {
		final PlayState playState = getPlayState();
		if (playState == PlayState.PAUSED) {
			schedulePlayStateChange(PlayState.PLAYING);
		}
		else if (playState == PlayState.PLAYING || playState == PlayState.LOADING) {
			schedulePlayStateChange(PlayState.PAUSED);
		}
		else if (playState == PlayState.STOPPED) {
			final PlayItem ci = getCurrentItem();
			if (ci != null) loadAndStartPlaying(ci);
		}
		else {
			LOG.warn("Asked to pause when state is {}, do not know what to do.", playState);
		}
	}

	@Override
	public void stopPlaying () {
		schedulePlayStateChange(PlayState.STOPPED);
	}

	@Override
	public void seekTo (final double seekToProportion) {
		final int durationSeconds = getCurrentTrackDuration();
		if (durationSeconds > 0) {
			final long seekToSeconds = (long) (durationSeconds * seekToProportion);
			this.eventQueue.add(Long.valueOf(seekToSeconds));
		}
	}

	// Visible for testing.
	@Override
	public void dlnaPlay (final PlayItem item, final String id, final String uri, final MimeType mimeType, final long fileSize, final int durationSeconds, final String coverArtUri) throws DlnaException {
		setCurrentItem(item);
		saveState();

		this.eventQueue.add(new DlnaToPlay(item, id, uri, mimeType, fileSize, durationSeconds, coverArtUri, this));
		this.eventQueue.add(PlayState.PLAYING);
		setStateToReportExternally(PlayState.LOADING);

		// Only restore position if for same item.
		final PlayerState rps = getRestorePositionState();
		if (rps != null && rps.getCurrentItem() != null && rps.getCurrentItem().hasTrack()) {
			if (Objs.equals(item.getTrack(), rps.getCurrentItem().getTrack())) {
				this.eventQueue.add(Long.valueOf(rps.getPosition()));
				LOG.info("Play scheduled restore position: {}s", rps.getPosition());
			}
			else {
				LOG.info("Not restoring position for {} as track is {}.",
						rps.getCurrentItem().getTrack(), item.getTrack());
			}
		}
		clearRestorePositionState();

		LOG.info("Playback scheduled: {}", id);
	}

}
