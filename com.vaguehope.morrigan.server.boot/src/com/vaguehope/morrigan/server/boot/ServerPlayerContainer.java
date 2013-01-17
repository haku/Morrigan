package com.vaguehope.morrigan.server.boot;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerContainer;
import com.vaguehope.morrigan.player.PlayerEventHandler;
import com.vaguehope.morrigan.server.ServerConfig;

class ServerPlayerContainer implements PlayerContainer {

	private final UiMgr uiMgr;
	private final NullScreen nullScreen;
	private final ServerConfig config;
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private LocalPlayer player;
	private ServerPlayerEventHandler eventHandler;

	public ServerPlayerContainer (UiMgr uiMgr, NullScreen nullScreen, ServerConfig config) {
		this.nullScreen = nullScreen;
		if (uiMgr == null) throw new IllegalArgumentException();
		this.uiMgr = uiMgr;
		this.config = config;
	}

	public void dispose () {
		LocalPlayer p = this.player;
		if (p != null) p.dispose();
	}

	@Override
	public String getName () {
		return "Server";
	}

	@Override
	public PlayerEventHandler getEventHandler () {
		if (this.eventHandler == null) {
			this.eventHandler = new ServerPlayerEventHandler(this.uiMgr, this, this.nullScreen);
		}
		return this.eventHandler;
	}

	@Override
	public void setPlayer (Player player) {
		if (!(player instanceof LocalPlayer)) throw new IllegalArgumentException("Only LocalPlayer supported.");
		this.player = (LocalPlayer) player;
		try {
			player.setPlaybackOrder(this.config.getPlaybackOrder());
		}
		catch (IOException e) {
			this.logger.log(Level.WARNING, "Failed to read playback order from config.", e);
			player.setPlaybackOrder(PlaybackOrder.STOP);
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
