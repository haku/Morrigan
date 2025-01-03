package com.vaguehope.morrigan.server.boot;

import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.LocalPlayerSupport;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerContainer;

public class ServerPlayerContainer implements PlayerContainer {

	private final String name;

	private LocalPlayer player;
	private ServerPlayerEventHandler localPlayerSupport;

	public ServerPlayerContainer (final String name) {
		this.name = name;
	}

	public void dispose () {
		LocalPlayer p = this.player;
		if (p != null) p.dispose();
		if (this.localPlayerSupport != null) this.localPlayerSupport.dispose();
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
	public LocalPlayerSupport getLocalPlayerSupport () {
		if (this.localPlayerSupport == null) {
			this.localPlayerSupport = new ServerPlayerEventHandler(this);
		}
		return this.localPlayerSupport;
	}

	@Override
	public void setPlayer (final Player player) {
		if (!(player instanceof LocalPlayer)) throw new IllegalArgumentException("Only LocalPlayer supported.");
		this.player = (LocalPlayer) player;
		this.player.addEventListener(this.localPlayerSupport);
	}

	public LocalPlayer getLocalPlayer () {
		return this.player;
	}

	@Override
	public Player getPlayer () {
		return this.player;
	}

}
