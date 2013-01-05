package com.vaguehope.morrigan.server.boot;

import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerContainer;
import com.vaguehope.morrigan.player.PlayerEventHandler;

class ServerPlayerContainer implements PlayerContainer {

	private final PlaybackOrder defaultPlaybackOrder;

	private Player player;
	private ServerPlayerEventHandler eventHandler;

	public ServerPlayerContainer (PlaybackOrder defaultPlaybackOrder) {
		this.defaultPlaybackOrder = defaultPlaybackOrder;
	}


	@Override
	public String getName () {
		return "Server";
	}

	@Override
	public PlayerEventHandler getEventHandler () {
		if (this.eventHandler == null || this.eventHandler.getPlayer() != this.player) {
			this.eventHandler = new ServerPlayerEventHandler(this.player);
		}
		return this.eventHandler;
	}

	@Override
	public void setPlayer (Player player) {
		this.player = player;
		player.setPlaybackOrder(this.defaultPlaybackOrder);
	}

	@Override
	public Player getPlayer () {
		return this.player;
	}

}