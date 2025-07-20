package com.vaguehope.morrigan.dlna.players;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import org.jupnp.model.ModelUtil;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.avtransport.AbstractAVTransportService;
import org.jupnp.support.lastchange.LastChange;
import org.jupnp.support.model.DeviceCapabilities;
import org.jupnp.support.model.MediaInfo;
import org.jupnp.support.model.PlayMode;
import org.jupnp.support.model.PositionInfo;
import org.jupnp.support.model.SeekMode;
import org.jupnp.support.model.StorageMedium;
import org.jupnp.support.model.TransportAction;
import org.jupnp.support.model.TransportInfo;
import org.jupnp.support.model.TransportSettings;
import org.jupnp.support.model.TransportState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.dlna.players.DlnaPlayingParamsFactory.DlnaPlayingParams;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.Player.PlayerEventListener;
import com.vaguehope.morrigan.transcode.Transcode;
import com.vaguehope.morrigan.util.ExceptionHelper;

public class PlayerControlBridgeAVTransportService extends AbstractAVTransportService {

	private static final Logger LOG = LoggerFactory.getLogger(PlayerControlBridgeAVTransportService.class);

	private final boolean vlog = true;

	private final Player player;
	private final DlnaPlayingParamsFactory dlnaPlayingParamsFactory;

	private volatile MediaInfo currentMediaInfo = new MediaInfo();

	public PlayerControlBridgeAVTransportService(final LastChange lastChange, final Player player, final DlnaPlayingParamsFactory dlnaPlayingParamsFactory) {
		super(lastChange);
		this.player = player;
		this.dlnaPlayingParamsFactory = dlnaPlayingParamsFactory;
		this.player.addEventListener(this.playerEventListener);
	}

	private final PlayerEventListener playerEventListener = new PlayerEventListener() {
		@Override
		public void currentItemChanged(final PlayItem newItem) {
			updateCurrentItemDetails(newItem);
		}
		@Override
		public void playStateChanged(final PlayState newPlayState, final PlayItem newItem) {
			notifySubscribers();
		}
		@Override
		public void playOrderChanged(final PlaybackOrder newPlaybackOrder) {
			notifySubscribers();
		}
		@Override
		public void afterSeek() {
			notifySubscribers();
		}

		@Override
		public void transcodeChanged(final Transcode newTranscode) {}
		@Override
		public void positionChanged(final long newPosition, final int duration) {}
		@Override
		public void onException(final Exception e) {}
	};

	private void updateCurrentItemDetails(final PlayItem newItem) {
		final PlayItem item = newItem != null ? newItem : this.player.getCurrentItem();

		if (item == null) {
			setCurrentMediaInfo(new MediaInfo());
			return;
		}

		if (this.player instanceof AbstractDlnaPlayer) {
			final DlnaPlayingParams params = ((AbstractDlnaPlayer) this.player).getCurrentPlayingParams();
			setCurrentMediaInfo(new MediaInfo(params.uri, params.asMetadata()));
			return;
		}

		try {
			final DlnaPlayingParams params = this.dlnaPlayingParamsFactory.make(item);
			setCurrentMediaInfo(new MediaInfo(params.uri, params.asMetadata()));
		}
		catch (final IOException | DlnaException e) {
			setCurrentMediaInfo(new MediaInfo());
			LOG.warn("Failed to generate DLNA matadata for currently playing item: {}", ExceptionHelper.causeTrace(e));
		}
	}

	private void setCurrentMediaInfo(final MediaInfo mediaInfo) {
		this.currentMediaInfo = mediaInfo;
		notifySubscribers();
	}

	private void notifySubscribers() {
		try {
			appendCurrentState(getLastChange(), getDefaultInstanceID());
		}
		catch (final Exception e) {
			LOG.warn("Failed to emit change event.", e);
		}
	}

	@Override
	public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
		return new UnsignedIntegerFourBytes[] { getDefaultInstanceID() };
	}

	@Override
	public void setAVTransportURI(final UnsignedIntegerFourBytes instanceId, final String currentURI, final String currentURIMetaData) throws AVTransportException {
		// for now setting specific track is not enabled, but could be added as a feature later maybe?
		throw new AVTransportException(AVTransportErrorCode.TRANSPORT_LOCKED);
	}

	@Override
	public void setNextAVTransportURI(final UnsignedIntegerFourBytes instanceId, final String nextURI, final String nextURIMetaData) throws AVTransportException {
		// ignore.
	}

	@Override
	public MediaInfo getMediaInfo(final UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		return this.currentMediaInfo;
	}

	@Override
	public TransportInfo getTransportInfo(final UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		final TransportState state;
		switch (this.player.getPlayState()) {
		case STOPPED:
			state = TransportState.STOPPED;
			break;
		case PLAYING:
			state = TransportState.PLAYING;
			break;
		case PAUSED:
			state = TransportState.PAUSED_PLAYBACK;
			break;
		case LOADING:
			state = TransportState.TRANSITIONING;
			break;
		default:
			state = TransportState.NO_MEDIA_PRESENT;
		}
		return new TransportInfo(state);
	}

	@Override
	public PositionInfo getPositionInfo(final UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		final int playerLength = this.player.getCurrentTrackDuration();
		final long playerTime = this.player.getCurrentPosition();

		final String duration;
		if (playerLength > 0) {
			duration = ModelUtil.toTimeString(playerLength);
		}
		else {
			duration = "00:00:00";
		}

		final String position;
		if (playerTime > 0) {
			position = ModelUtil.toTimeString(playerTime);
		}
		else {
			position = "00:00:00";
		}

		final MediaInfo mediaInfo = this.currentMediaInfo;

		String trackUri = mediaInfo.getCurrentURI();
		if (trackUri == null) trackUri = "";

		String trackMetaData = mediaInfo.getCurrentURIMetaData();
		if (trackMetaData == null) trackMetaData = "NOT_IMPLEMENTED";

		return new PositionInfo(1, duration, trackMetaData, trackUri, position, position, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public DeviceCapabilities getDeviceCapabilities(final UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		return new DeviceCapabilities(new StorageMedium[] { });
	}

	@Override
	protected TransportAction[] getCurrentTransportActions(final UnsignedIntegerFourBytes instanceId) throws Exception {
		final Set<TransportAction> actions = EnumSet.noneOf(TransportAction.class);

		switch (this.player.getPlayState()) {
		case STOPPED:
			actions.add(TransportAction.Play);
			actions.add(TransportAction.Next);
			break;
		case LOADING:
		case PLAYING:
			actions.add(TransportAction.Pause);
			actions.add(TransportAction.Stop);
			actions.add(TransportAction.Seek);
			actions.add(TransportAction.Next);
			break;
		case PAUSED:
			actions.add(TransportAction.Play);
			actions.add(TransportAction.Pause); // so HA can pause an already paused player.
			actions.add(TransportAction.Stop);
			actions.add(TransportAction.Seek);
			actions.add(TransportAction.Next);
			break;
		default:
		}

		return actions.toArray(new TransportAction[actions.size()]);
	}

	@Override
	public void stop(final UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		if (this.vlog) LOG.info("stop({})", instanceId);
		this.player.stopPlaying();
	}

	@Override
	public void play(final UnsignedIntegerFourBytes instanceId, final String speed) throws AVTransportException {
		if (this.vlog) LOG.info("play({})", instanceId);
		this.player.pausePlaying();
	}

	@Override
	public void pause(final UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		if (this.vlog) LOG.info("pause({})", instanceId);
		this.player.pausePlaying();
	}

	@Override
	public void next(final UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		if (this.vlog) LOG.info("TODO next({})", instanceId);
		this.player.nextTrack();
	}

	@Override
	public void previous(final UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		// ignore.
	}

	@Override
	public void seek(final UnsignedIntegerFourBytes instanceId, final String unit, final String target) throws AVTransportException {
		if (this.vlog) LOG.info("seek({}, {}, {})", instanceId, unit, target);

		final SeekMode seekMode = SeekMode.valueOrExceptionOf(unit);
		if (!seekMode.equals(SeekMode.REL_TIME)) {
			throw new AVTransportException(ErrorCode.INVALID_ARGS, "Unsupported SeekMode: " + unit);
		}

		final long targetSeconds = ModelUtil.fromTimeString(target);
		final int duration = this.player.getCurrentTrackDuration();
		this.player.seekTo(targetSeconds / (double) duration); // FIXME WTF was I thinking when I wrote this API?
	}

	@Override
	public TransportSettings getTransportSettings(final UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		// TODO support SHUFFLE and map to something in PlaybackOrder ?
		return new TransportSettings(PlayMode.NORMAL);
	}

	@Override
	public void setPlayMode(final UnsignedIntegerFourBytes instanceId, final String newPlayMode) throws AVTransportException {
		// TODO other half of getTransportSettings()
		if (this.vlog) LOG.info("TODO setPlayMode({}, {})", instanceId, newPlayMode);
	}

	@Override
	public void setRecordQualityMode(final UnsignedIntegerFourBytes instanceId, final String newRecordQualityMode) throws AVTransportException {
		// ignore.
	}

	@Override
	public void record(final UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		// ignore.
	}

}
