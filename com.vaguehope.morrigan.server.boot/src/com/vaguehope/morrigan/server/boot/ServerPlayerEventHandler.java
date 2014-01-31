package com.vaguehope.morrigan.server.boot;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.LocalPlayerSupport;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.Player.PlayerEventListener;
import com.vaguehope.morrigan.screen.ScreenMgr;

class ServerPlayerEventHandler implements PlayerEventListener, LocalPlayerSupport {

	private static final Logger logger = Logger.getLogger(ServerPlayerEventHandler.class.getName());

	private final UiMgr uiMgr;
	private final ServerPlayerContainer playerContainer;
	private final NullScreen nullScreen;
	private final ExecutorService executorService;

	private ScreenRegister screenRegister;
	private ScreenMgr screenMgr;

	private final AtomicReference<PlayState> prevPlayState = new AtomicReference<PlayState>();


	public ServerPlayerEventHandler (final UiMgr uiMgr, final ServerPlayerContainer playerContainer, final NullScreen nullScreen, final ExecutorService executorService) {
		if (uiMgr == null) throw new IllegalArgumentException();
		if (playerContainer == null) throw new IllegalArgumentException();
		if (nullScreen == null) throw new IllegalArgumentException();
		if (executorService == null) throw new IllegalArgumentException();
		this.uiMgr = uiMgr;
		this.playerContainer = playerContainer;
		this.nullScreen = nullScreen;
		this.executorService = executorService;
	}

	public synchronized void dispose() {
		if (this.screenRegister != null) this.screenRegister.dispose();
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

	private static String getPlayerStateDescription (final Player p) {
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

	private synchronized ScreenMgr getScreenMgr() {
		if (this.screenMgr == null) {
			final Display display = this.uiMgr.getDisplay();
			if (display != null) {
				if (this.screenRegister == null) {
					this.screenRegister = new ScreenRegister(display, new PlayerTitleProvider(this.playerContainer), this.executorService);
				}
				this.screenMgr = new ScreenMgr(display, this.screenRegister, new ServerScreenMgrCallback(this, this.nullScreen));
			}
		}
		return this.screenMgr;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void playOrderChanged (final PlaybackOrder newPlaybackOrder) {
		outputStatus();
	}

	@Override
	public void currentItemChanged (final PlayItem newItem) {
		outputStatus();
		if (this.screenRegister != null) this.screenRegister.updateTitle();
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
		logger.log(Level.WARNING, "asyncThrowable", e);
	}

	@Override
	public Composite getCurrentMediaFrameParent () {
		final ScreenMgr sm = getScreenMgr();
		if (sm == null) return null;
		return sm.getCurrentVideoParent();
	}

	@Override
	public Map<Integer, String> getMonitors () {
		return this.uiMgr.getMonitorNames();
	}

	@Override
	public void goFullscreen (final int monitorIndex) {
		final ScreenMgr sm = getScreenMgr();
		if (sm == null) {
			logger.log(Level.WARNING, "Can not go full screen as UI not avaible.");
			return;
		}
		this.screenMgr.goFullScreenSafe(this.uiMgr.getMonitor(monitorIndex));
	}

	@Override
	public IMediaTrackList<IMediaTrack> getCurrentList () {
		return null;
	}

	@Override
	public void historyChanged () {/* UNUSED */}

	@Override
	public void videoAreaSelected () {/* UNUSED */}

	@Override
	public void videoAreaClose () {/* UNUSED */}

}
