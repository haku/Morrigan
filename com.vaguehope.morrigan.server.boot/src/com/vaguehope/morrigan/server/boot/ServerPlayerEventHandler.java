package com.vaguehope.morrigan.server.boot;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Monitor;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerEventHandler;
import com.vaguehope.morrigan.screen.ScreenMgr;

class ServerPlayerEventHandler implements PlayerEventHandler {

	private static final Logger logger = Logger.getLogger(ServerPlayerEventHandler.class.getName());

	private final UiMgr uiMgr;
	private final ServerPlayerContainer playerContainer;
	private final ScreenRegister screenRegister;
	private final ScreenMgr screenMgr;

	private AtomicReference<PlayState> prevPlayState = new AtomicReference<PlayState>();

	public ServerPlayerEventHandler (UiMgr uiMgr, ServerPlayerContainer playerContainer, NullScreen nullScreen) {
		if (uiMgr == null) throw new IllegalArgumentException();
		if (playerContainer == null) throw new IllegalArgumentException();
		this.uiMgr = uiMgr;
		this.playerContainer = playerContainer;
		if (this.uiMgr.getDisplay() != null) {
			this.screenRegister = new ScreenRegister(this.uiMgr.getDisplay(), new PlayerTitleProvider(playerContainer));
			this.screenMgr = new ScreenMgr(uiMgr.getDisplay(), this.screenRegister, new ServerScreenMgrCallback(this, nullScreen));
		}
		else {
			this.screenRegister = null;
			this.screenMgr = null;
		}
	}

	public LocalPlayer getPlayer () {
		return this.playerContainer.getLocalPlayer();
	}

	private void outputStatus () {
		Player player = this.playerContainer.getPlayer();
		PlayState currentState = (player == null ? null : player.getPlayState());
		if (currentState != this.prevPlayState.get()) {
			this.prevPlayState.set(currentState);
			System.out.println(getPlayerStateDescription(player));
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
		logger.log(Level.WARNING, "asyncThrowable", t);
	}

	@Override
	public Composite getCurrentMediaFrameParent () {
		if (this.screenMgr == null) return null;
		return this.screenMgr.getCurrentVideoParent();
	}

	@Override
	public Map<Integer, String> getMonitors () {
		return this.uiMgr.getMonitorNames();
	}

	@Override
	public void goFullscreen (int monitorIndex) {
		if (this.screenMgr == null) return;
		Monitor monitor = this.uiMgr.getMonitor(monitorIndex);
		this.screenMgr.goFullScreenSafe(monitor);
	}

	@Override
	public IMediaTrackList<IMediaTrack> getCurrentList () {
		return null;
	}

	@Override
	public void currentItemChanged () {
		if (this.screenRegister == null) return;
		this.screenRegister.updateTitle();
	}

	@Override
	public void historyChanged () {/* UNUSED */}

	@Override
	public void videoAreaSelected () {/* UNUSED */}

	@Override
	public void videoAreaClose () {/* UNUSED */}
}
