package com.vaguehope.morrigan.dlna.players;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.support.model.TransportInfo;
import org.jupnp.support.model.TransportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.dlna.UpnpHelper;
import com.vaguehope.morrigan.dlna.players.DlnaPlayingParamsFactory.DlnaPlayingParams;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.player.AbstractPlayer;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.PlayerStateStorage;

public abstract class AbstractDlnaPlayer extends AbstractPlayer {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractDlnaPlayer.class);

	protected final ControlPoint controlPoint;
	protected final RemoteService avTransportSvc;

	protected final AvTransportActions avTransport;
	protected final RenderingControlActions renderingControl;
	protected final ScheduledExecutorService scheduledExecutor;
	private final DlnaPlayingParamsFactory dlnaPlayingParamsFactory;

	private final String uid;

	protected final PlayerEventCache playerEventCache = new PlayerEventCache();

	private final AtomicReference<DlnaPlayingParams> currentPlayingParams = new AtomicReference<>();

	private volatile PlayerState restorePositionState;

	public AbstractDlnaPlayer (
			final PlayerRegister register,
			final ControlPoint controlPoint,
			final RemoteService avTransportSvc,
			final DlnaPlayingParamsFactory dlnaPlayingParamsFactory,
			final ScheduledExecutorService scheduledExecutor,
			final PlayerStateStorage playerStateStorage,
			final Config config,
			final AvTransportActions avTransportActions,
			final RenderingControlActions renderingControlActions) {
		super(UpnpHelper.idFromRemoteService(avTransportSvc), avTransportSvc.getDevice().getDetails().getFriendlyName(), register, playerStateStorage, config);
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

		this.dlnaPlayingParamsFactory = dlnaPlayingParamsFactory;
		this.scheduledExecutor = scheduledExecutor;
		this.uid = UpnpHelper.remoteServiceUid(avTransportSvc);
		addEventListener(this.playerEventCache);
	}

	public String getUid () {
		return this.uid;
	}

	public RemoteService getRemoteService() {
		return this.avTransportSvc;
	}

	@Override
	protected void onDispose () {
		LOG.info("Disposed {}: {}.", this.uid, toString());
	}

	@Override
	public boolean isPlaybackEngineReady () {
		return !isDisposed();
	}

	public void setCurrentPlayingParams(final DlnaPlayingParams playingParams) {
		this.currentPlayingParams.set(playingParams);
	}

	public DlnaPlayingParams getCurrentPlayingParams() {
		return this.currentPlayingParams.get();
	}

	@Override
	public long getCurrentPosition () {
		return this.playerEventCache.getPosition();
	}

	@Override
	public int getCurrentTrackDurationFromRenderer () {
		return this.playerEventCache.getDuration();
	}

	@Override
	protected void loadAndPlay (final PlayItem item) throws DlnaException, IOException {
		final DlnaPlayingParams playingParams = this.dlnaPlayingParamsFactory.make(item);
		dlnaPlay(item, playingParams);

		// After dlnaPlay() because it will (likely) call setCurrentItem().
		setCurrentTrackDurationAsMeasured(playingParams.durationSeconds);
	}

	protected abstract void dlnaPlay (PlayItem item, DlnaPlayingParams playingParams) throws DlnaException;

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
			default:
				break;
			}
		}
		return PlayState.STOPPED;
	}

}
