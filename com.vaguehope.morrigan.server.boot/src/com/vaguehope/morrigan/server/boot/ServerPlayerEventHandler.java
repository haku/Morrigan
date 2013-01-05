package com.vaguehope.morrigan.server.boot;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerEventHandler;

class ServerPlayerEventHandler implements PlayerEventHandler {

	private final UiMgr uiMgr;
	private final Player player;

	private AtomicReference<PlayState> prevPlayState = new AtomicReference<PlayState>();

	public ServerPlayerEventHandler (UiMgr uiMgr, Player player) {
		this.uiMgr = uiMgr;
		this.player = player;
	}

	public Player getPlayer () {
		return this.player;
	}

	private void outputStatus () {
		PlayState currentState = (this.player == null ? null : this.player.getPlayState());
		if (currentState != this.prevPlayState.get()) {
			this.prevPlayState.set(currentState);
			System.out.println(getPlayerStateDescription(this.player));
		}
	}

	private static String getPlayerStateDescription (Player p) {
		if (p != null) {
			PlayState currentState = p.getPlayState();
			if (currentState != null) {
				PlayItem currentPlayItem = p.getCurrentItem();
				IMediaTrack currentItem = (currentPlayItem != null ? currentPlayItem.item : null);
				if (currentItem != null) {
					return currentState + " " + currentItem + ".";
				}
				return currentState + ".";
			}
			return "Unknown.";
		}
		return "Player unset.";
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void updateStatus () {
		outputStatus();
	}

	@Override
	public void asyncThrowable (Throwable t) {
		Activator.logger.log(Level.WARNING, "asyncThrowable", t);
	}

	@Override
	public Composite getCurrentMediaFrameParent () {
		return null;
	}

	@Override
	public Map<Integer, String> getMonitors () {
		return this.uiMgr.getMonitorNames();
	}

	@Override
	public void goFullscreen (int monitor) {/* UNUSED */}

	@Override
	public IMediaTrackList<IMediaTrack> getCurrentList () {
		return null;
	}

	@Override
	public void currentItemChanged () {/* UNUSED */}

	@Override
	public void historyChanged () {/* UNUSED */}

	@Override
	public void videoAreaSelected () {/* UNUSED */}

	@Override
	public void videoAreaClose () {/* UNUSED */}
}