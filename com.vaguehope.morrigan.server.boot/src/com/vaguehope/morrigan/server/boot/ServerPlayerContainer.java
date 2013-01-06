package com.vaguehope.morrigan.server.boot;

import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerContainer;
import com.vaguehope.morrigan.player.PlayerEventHandler;

class ServerPlayerContainer implements PlayerContainer {

	private final UiMgr uiMgr;
	private final NullScreen nullScreen;
	private final PlaybackOrder defaultPlaybackOrder;

	private LocalPlayer player;
	private ServerPlayerEventHandler eventHandler;

	public ServerPlayerContainer (UiMgr uiMgr, NullScreen nullScreen, PlaybackOrder defaultPlaybackOrder) {
		this.nullScreen = nullScreen;
		if (uiMgr == null) throw new IllegalArgumentException();
		this.uiMgr = uiMgr;
		this.defaultPlaybackOrder = defaultPlaybackOrder;
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
		player.setPlaybackOrder(this.defaultPlaybackOrder);
	}

	public LocalPlayer getLocalPlayer () {
		return this.player;
	}

	@Override
	public Player getPlayer () {
		return this.player;
	}

}
