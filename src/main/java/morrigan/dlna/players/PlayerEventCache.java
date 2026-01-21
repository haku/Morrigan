package morrigan.dlna.players;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import morrigan.engines.playback.IPlaybackEngine.PlayState;
import morrigan.player.PlayItem;
import morrigan.player.PlaybackOrder;
import morrigan.player.Player.PlayerEventListener;
import morrigan.transcode.Transcode;

public class PlayerEventCache implements PlayerEventListener {

	private static final Logger LOG = LoggerFactory.getLogger(PlayerEventCache.class);

	private volatile PlayState playState;
	private volatile long position = -1;
	private volatile int duration = -1;

	@Override
	public void playOrderChanged(final PlaybackOrder newPlaybackOrder) {}

	@Override
	public void transcodeChanged(final Transcode newTranscode) {}

	@Override
	public void currentItemChanged(final PlayItem newItem) {}

	@Override
	public void playStateChanged(final PlayState newPlayState, final PlayItem newItem) {
		this.playState = newPlayState;
	}

	@Override
	public void positionChanged(final long newPosition, final int newDuration) {
		this.position = newPosition;
		this.duration = newDuration;
	}

	@Override
	public void afterSeek() {}

	@Override
	public void onException(final Exception e) {
		// TODO propagate to the UI!
		LOG.warn("Unhandled excpetion.", e);
	}

	public PlayState getPlayState() {
		return this.playState;
	}

	public long getPosition() {
		return this.position;
	}

	public int getDuration() {
		return this.duration;
	}

}
