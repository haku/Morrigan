package com.vaguehope.morrigan.server.boot;

import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerContainer;

public class ServerPlayerContainer implements PlayerContainer {

	private final String name;
	private final ServerPlayerEventHandler serverPlayerEventHandler;

	private Player player;

	public ServerPlayerContainer (final String name) {
		this.name = name;
		this.serverPlayerEventHandler = new ServerPlayerEventHandler(this);
	}

	public void dispose () {
		Player p = this.player;
		if (p != null) p.dispose();
		this.serverPlayerEventHandler.dispose();
	}

	@Override
	public String getPrefix () {
		return "s";
	}

	@Override
	public String getName () {
		return this.name;
	}

	@Override
	public void setPlayer (final Player player) {
		if (!(player instanceof LocalPlayer)) throw new IllegalArgumentException("Only LocalPlayer supported.");
		this.player = player;
		this.player.addEventListener(this.serverPlayerEventHandler);
	}

	@Override
	public Player getPlayer () {
		return this.player;
	}

}
