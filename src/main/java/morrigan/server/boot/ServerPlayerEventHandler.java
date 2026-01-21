package morrigan.server.boot;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import morrigan.engines.playback.IPlaybackEngine.PlayState;
import morrigan.player.PlayItem;
import morrigan.player.PlaybackOrder;
import morrigan.player.Player;
import morrigan.player.Player.PlayerEventListener;
import morrigan.transcode.Transcode;

class ServerPlayerEventHandler implements PlayerEventListener {

	private static final Logger logger = Logger.getLogger(ServerPlayerEventHandler.class.getName());

	private final ServerPlayerContainer playerContainer;

	private final AtomicReference<PlayState> prevPlayState = new AtomicReference<>();


	public ServerPlayerEventHandler (final ServerPlayerContainer playerContainer) {
		if (playerContainer == null) throw new IllegalArgumentException();
		this.playerContainer = playerContainer;
	}

	public synchronized void dispose() {
	}

	public Player getPlayer () {
		return this.playerContainer.getPlayer();
	}

	private void outputStatus (final PlayState playState, final PlayItem item) {
		final Player player = this.playerContainer.getPlayer();

		PlayState ps = playState;
		if (ps == null && player != null) ps = player.getPlayState();

		PlayItem i = item;
		if (i == null && player != null) i = player.getCurrentItem();

		if (ps != this.prevPlayState.get()) {
			this.prevPlayState.set(ps);

			String msg = "";
			if (ps != null) msg += ps;
			if (i != null) {
				msg += " " + i.getTitle();
			}

			logger.log(Level.INFO, msg);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void playOrderChanged (final PlaybackOrder newPlaybackOrder) {
		// unused.
	}

	@Override
	public void transcodeChanged (final Transcode newTranscode) {
		// unused.
	}

	@Override
	public void currentItemChanged (final PlayItem newItem) {
		outputStatus(null, newItem);
	}

	@Override
	public void playStateChanged (final PlayState newPlayState, final PlayItem newItem) {
		outputStatus(newPlayState, newItem);
	}

	@Override
	public void positionChanged (final long newPosition, final int duration) {
		// unused.
	}

	@Override
	public void afterSeek() {
		// unused.
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void onException (final Exception e) {
		// TODO propagate to the UI!
		logger.log(Level.WARNING, "asyncThrowable", e);
	}

}
