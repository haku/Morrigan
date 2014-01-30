package com.vaguehope.morrigan.server.boot;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerContainer;
import com.vaguehope.morrigan.player.LocalPlayerSupport;
import com.vaguehope.morrigan.server.ServerConfig;

class ServerPlayerContainer implements PlayerContainer {

	private final UiMgr uiMgr;
	private final NullScreen nullScreen;
	private final ServerConfig config;
	private final ExecutorService executorService;
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private LocalPlayer player;
	private ServerPlayerEventHandler localPlayerSupport;

	public ServerPlayerContainer (final UiMgr uiMgr, final NullScreen nullScreen, final ServerConfig config, final ExecutorService executorService) {
		if (uiMgr == null) throw new IllegalArgumentException();
		if (nullScreen == null) throw new IllegalArgumentException();
		if (config == null) throw new IllegalArgumentException();
		if (executorService == null) throw new IllegalArgumentException();
		this.uiMgr = uiMgr;
		this.nullScreen = nullScreen;
		this.config = config;
		this.executorService = executorService;
	}

	public void dispose () {
		LocalPlayer p = this.player;
		if (p != null) p.dispose();
		if (this.localPlayerSupport != null) this.localPlayerSupport.dispose();
	}

	@Override
	public String getName () {
		return "Server";
	}

	@Override
	public LocalPlayerSupport getLocalPlayerSupport () {
		if (this.localPlayerSupport == null) {
			this.localPlayerSupport = new ServerPlayerEventHandler(this.uiMgr, this, this.nullScreen, this.executorService);
		}
		return this.localPlayerSupport;
	}

	@Override
	public void setPlayer (final Player player) {
		if (!(player instanceof LocalPlayer)) throw new IllegalArgumentException("Only LocalPlayer supported.");
		this.player = (LocalPlayer) player;
		this.player.addEventListener(this.localPlayerSupport);
		try {
			player.setPlaybackOrder(this.config.getPlaybackOrder());
		}
		catch (IOException e) {
			this.logger.log(Level.WARNING, "Failed to read playback order from config.", e);
			player.setPlaybackOrder(PlaybackOrder.MANUAL);
		}
	}

	public LocalPlayer getLocalPlayer () {
		return this.player;
	}

	@Override
	public Player getPlayer () {
		return this.player;
	}

}
