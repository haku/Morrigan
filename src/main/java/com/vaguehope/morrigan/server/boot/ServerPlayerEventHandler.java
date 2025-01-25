package com.vaguehope.morrigan.server.boot;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.Player.PlayerEventListener;
import com.vaguehope.morrigan.transcode.Transcode;

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
		return this.playerContainer.getLocalPlayer();
	}

	private void outputStatus (final PlayItem newItem) {
		final Player player = this.playerContainer.getPlayer();
		final PlayState currentState = (player == null ? null : player.getPlayState());
		if (currentState != this.prevPlayState.get()) {
			this.prevPlayState.set(currentState);
			logger.log(Level.INFO, getPlayerStateDescription(player, newItem));
		}
	}

	private static String getPlayerStateDescription (final Player p, final PlayItem newItem) {
		if (p != null) {
			final PlayState currentState = p.getPlayState();
			if (currentState != null) {
				final PlayItem playItem = newItem != null ? newItem : p.getCurrentItem();
				if (playItem != null) return currentState + " " + playItem.getTitle() + ".";
				return currentState + ".";
			}
			return "Unknown.";
		}
		return "Player unset.";
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void playOrderChanged (final PlaybackOrder newPlaybackOrder) {
		outputStatus(null);
	}

	@Override
	public void transcodeChanged (final Transcode newTranscode) {
		outputStatus(null);
	}

	@Override
	public void currentItemChanged (final PlayItem newItem) {
		outputStatus(newItem);
	}

	@Override
	public void playStateChanged (final PlayState newPlayState) {
		outputStatus(null);
	}

	@Override
	public void positionChanged (final long newPosition, final int duration) {
		outputStatus(null);
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
