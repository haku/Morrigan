package com.vaguehope.morrigan.dlna.players;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportStatus;
import org.seamless.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.dlna.MediaFormat;
import com.vaguehope.morrigan.dlna.UpnpHelper;
import com.vaguehope.morrigan.dlna.content.MediaFileLocator;
import com.vaguehope.morrigan.dlna.httpserver.MediaServer;
import com.vaguehope.morrigan.dlna.util.StringHelper;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.AbstractPlayer;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.transcode.FfprobeCache;
import com.vaguehope.morrigan.util.Objs;

public abstract class AbstractDlnaPlayer extends AbstractPlayer {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractDlnaPlayer.class);

	protected final ControlPoint controlPoint;
	protected final RemoteService avTransportSvc;

	protected final AvTransportActions avTransport;
	protected final RenderingControlActions renderingControl;
	protected final ScheduledExecutorService scheduledExecutor;
	private final MediaServer mediaServer;
	private final MediaFileLocator mediaFileLocator;

	private final String uid;

	protected final PlayerEventCache playerEventCache = new PlayerEventCache();

	private final AtomicReference<PlayItem> currentItem = new AtomicReference<>();
	private final AtomicInteger currentItemDurationSeconds = new AtomicInteger(-1);

	private volatile PlayerState restorePositionState;

	public AbstractDlnaPlayer (
			final PlayerRegister register,
			final ControlPoint controlPoint,
			final RemoteService avTransportSvc,
			final MediaServer mediaServer,
			final MediaFileLocator mediaFileLocator,
			final ScheduledExecutorService scheduledExecutor,
			final AvTransportActions avTransportActions,
			final RenderingControlActions renderingControlActions) {
		super(UpnpHelper.idFromRemoteService(avTransportSvc), avTransportSvc.getDevice().getDetails().getFriendlyName(), register);
		this.controlPoint = controlPoint;
		this.avTransportSvc = avTransportSvc;

		if (avTransportActions != null) {
			this.avTransport = avTransportActions;
		}
		else {
			this.avTransport = new AvTransportActions(controlPoint, avTransportSvc);
		}

		if (renderingControlActions != null) {
			this.renderingControl = renderingControlActions;
		}
		else {
			final RemoteService renderingControlSvc = UpnpHelper.findFirstServiceOfType(avTransportSvc.getDevice(), UpnpHelper.SERVICE_RENDERINGCONTROL);
			if (renderingControlSvc != null) {
				this.renderingControl = new RenderingControlActions(controlPoint, renderingControlSvc);
			}
			else {
				this.renderingControl = null;
			}
		}

		this.mediaServer = mediaServer;
		this.mediaFileLocator = mediaFileLocator;
		this.scheduledExecutor = scheduledExecutor;
		this.uid = UpnpHelper.remoteServiceUid(avTransportSvc);
		addEventListener(this.playerEventCache);
	}

	public String getUid () {
		return this.uid;
	}

	@Override
	protected void onDispose () {
		LOG.info("Disposed {}: {}.", this.uid, toString());
	}

	@Override
	public boolean isPlaybackEngineReady () {
		return !isDisposed();
	}

	@Override
	public List<PlayItem> getHistory () {
		return Collections.emptyList();
	}

	@Override
	public IMediaTrackList<? extends IMediaTrack> getCurrentList () {
		final PlayItem item = getCurrentItem();
		return item == null ? null : item.getList();
	}

	@Override
	public void setCurrentItem (final PlayItem item) {
		final PlayItem old = this.currentItem.getAndSet(item);
		if (!Objs.equals(old, item)) {
			this.currentItemDurationSeconds.set(-1);
		}
	}

	@Override
	public PlayItem getCurrentItem () {
		return this.currentItem.get();
	}

	@Override
	public long getCurrentPosition () {
		return this.playerEventCache.getPosition();
	}

	@Override
	public int getCurrentTrackDurationAsMeasured () {
		return this.currentItemDurationSeconds.get();
	}

	@Override
	public int getCurrentTrackDurationFromRenderer () {
		return this.playerEventCache.getDuration();
	}

	@Override
	public void nextTrack () {
		checkAlive();
		final PlayItem nextItemToPlay = findNextItemToPlay();
		if (nextItemToPlay != null) {
			loadAndStartPlaying(nextItemToPlay);
		}
		else {
			stopPlaying();
		}
	}

	@Override
	protected void loadAndPlay (final PlayItem item, final File altFile) throws DlnaException, IOException {
		final String id;
		if (altFile != null) {
			id = this.mediaFileLocator.fileId(altFile);
		}
		else if (StringHelper.notBlank(item.getTrack().getRemoteId())) {
			id = item.getTrack().getRemoteId();
		}
		else {
			id = this.mediaFileLocator.fileId(new File(item.getTrack().getFilepath()));
		}

		final String uri;
		final MimeType mimeType;
		final long fileSize;
		final int durationSeconds;
		if (altFile != null) {
			uri = this.mediaServer.uriForId(id);
			mimeType = MediaFormat.identify(altFile).toMimeType();
			fileSize = altFile.length();
			durationSeconds = readFileDurationSeconds(altFile);
		}
		else if (StringHelper.notBlank(item.getTrack().getRemoteLocation())) {
			uri = item.getTrack().getRemoteLocation();
			mimeType = MimeType.valueOf(item.getTrack().getMimeType());
			fileSize = item.getTrack().getFileSize();
			durationSeconds = item.getTrack().getDuration(); // TODO what if this is not available?
		}
		else {
			uri = this.mediaServer.uriForId(id);
			final File file = new File(item.getTrack().getFilepath());
			mimeType = MediaFormat.identify(file).toMimeType();
			fileSize = file.length();
			int d = item.getTrack().getDuration();
			if (d < 1) d = readFileDurationSeconds(file);
			durationSeconds = d;
		}

		if (durationSeconds < 1) throw new DlnaException("Can not play track without a known duration.");

		final String coverArtUri;
		if (StringHelper.notBlank(item.getTrack().getCoverArtRemoteLocation())) {
			coverArtUri = item.getTrack().getCoverArtRemoteLocation();
		}
		else {
			final File coverArt = item.getTrack().findCoverArt();
			coverArtUri = coverArt != null ? this.mediaServer.uriForId(this.mediaFileLocator.fileId(coverArt)) : null;
		}

		dlnaPlay(item, id, uri, mimeType, fileSize, durationSeconds, coverArtUri);

		// After dlnaPlay() because it will (likely) call setCurrentItem().
		this.currentItemDurationSeconds.set(durationSeconds);
	}

	/**
	 * Returns valid duration or throws.
	 */
	private static int readFileDurationSeconds (final File altFile) throws IOException {
		final Long fileDurationMillis = FfprobeCache.inspect(altFile).getDurationMillis();
		if (fileDurationMillis == null || fileDurationMillis < 1) throw new IOException("Failed to read file duration: " + altFile.getAbsolutePath());
		LOG.info("Duration {}ms: {}", fileDurationMillis, altFile.getAbsolutePath());
		final int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(fileDurationMillis);
		return seconds < 1 ? 1 : seconds; // 0ms < d < 1s gets rounded up to 1s.
	}

	protected abstract void dlnaPlay (PlayItem item, String id, String uri, MimeType mimeType, long fileSize, int durationSeconds, String coverArtUri) throws DlnaException;

	protected abstract boolean shouldBePlaying ();

	public PlayerState backupState () {
		return new PlayerState(getPlaybackOrder(), getTranscode(), getCurrentItem(), getCurrentPosition(), shouldBePlaying(), getQueue());
	}

	void restoreBackedUpState (final PlayerState state) {
		if (state == null) return;
		setPlaybackOrder(state.getPlaybackOrder());
		setTranscode(state.getTranscode());
		setCurrentItem(state.getCurrentItem());
		this.restorePositionState = state;
		state.addItemsToQueue(getQueue());

		if (state.isPlaying() && state.getCurrentItem() != null) {
			loadAndStartPlaying(state.getCurrentItem());
		}

		markStateRestoreAttempted();
		LOG.info("Restored {}: {}.", getUid(), state);
	}

	protected PlayerState getRestorePositionState () {
		return this.restorePositionState;
	}

	protected void clearRestorePositionState () {
		this.restorePositionState = null;
	}

	protected void recordTrackStarted (final PlayItem item) {
		this.scheduledExecutor.execute(new RecordTrackStarted(item));
	}

	protected void recordTrackCompleted (final PlayItem item) {
		this.scheduledExecutor.execute(new RecordTrackCompleted(item));
	}

	public static PlayState transportIntoToPlayState (final TransportInfo ti) {
		if (ti == null) return PlayState.STOPPED;
		if (ti.getCurrentTransportStatus() == TransportStatus.OK) {
			switch (ti.getCurrentTransportState()) {
				case PLAYING:
				case RECORDING:
					return PlayState.PLAYING;
				case PAUSED_PLAYBACK:
				case PAUSED_RECORDING:
					return PlayState.PAUSED;
				case TRANSITIONING:
				case CUSTOM:
					return PlayState.LOADING;
				case STOPPED:
				case NO_MEDIA_PRESENT:
					return PlayState.STOPPED;
			}
		}
		return PlayState.STOPPED;
	}

}
