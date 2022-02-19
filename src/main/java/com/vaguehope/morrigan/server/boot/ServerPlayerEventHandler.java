package com.vaguehope.morrigan.server.boot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.LocalPlayerSupport;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.Player.PlayerEventListener;
import com.vaguehope.morrigan.transcode.Transcode;

class ServerPlayerEventHandler implements PlayerEventListener, LocalPlayerSupport {

	private static final Logger logger = Logger.getLogger(ServerPlayerEventHandler.class.getName());

	private final ServerPlayerContainer playerContainer;
	private final ExecutorService executorService;

	private final AtomicReference<PlayState> prevPlayState = new AtomicReference<PlayState>();


	public ServerPlayerEventHandler (final ServerPlayerContainer playerContainer, final ExecutorService executorService) {
		if (playerContainer == null) throw new IllegalArgumentException();
		if (executorService == null) throw new IllegalArgumentException();
		this.playerContainer = playerContainer;
		this.executorService = executorService;
	}

	public synchronized void dispose() {
	}

	public LocalPlayer getPlayer () {
		return this.playerContainer.getLocalPlayer();
	}

	private void outputStatus () {
		final Player player = this.playerContainer.getPlayer();
		final PlayState currentState = (player == null ? null : player.getPlayState());
		if (currentState != this.prevPlayState.get()) {
			this.prevPlayState.set(currentState);
			logger.log(Level.INFO, getPlayerStateDescription(player));
		}
	}

	private static String getPlayerStateDescription (final Player p) {
		if (p != null) {
			final PlayState currentState = p.getPlayState();
			if (currentState == PlayState.LOADING) return currentState + "...";

			if (currentState != null) {
				final PlayItem currentPlayItem = p.getCurrentItem();
				final IMediaTrack track = (currentPlayItem != null ? currentPlayItem.getTrack() : null);
				if (track != null) return currentState + " " + track + ".";
				return currentState + ".";
			}
			return "Unknown.";
		}
		return "Player unset.";
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void playOrderChanged (final PlaybackOrder newPlaybackOrder) {
		outputStatus();
	}

	@Override
	public void transcodeChanged (final Transcode newTranscode) {
		outputStatus();
	}

	@Override
	public void currentItemChanged (final PlayItem newItem) {
		outputStatus();
	}

	@Override
	public void playStateChanged (final PlayState newPlayState) {
		outputStatus();
	}

	@Override
	public void positionChanged (final long newPosition, final int duration) {
		outputStatus();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void onException (final Exception e) {
		// TODO propagate to the UI!
		logger.log(Level.WARNING, "asyncThrowable", e);
	}

	@Override
	public IMediaTrackList<IMediaTrack> getCurrentList () {
		return null;
	}

	@Override
	public void historyChanged () {/* UNUSED */}

}
