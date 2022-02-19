package com.vaguehope.morrigan.server.boot;

import java.util.concurrent.ExecutorService;

import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.LocalPlayerSupport;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerContainer;

public class ServerPlayerContainer implements PlayerContainer {

	private final ExecutorService executorService;

	private LocalPlayer player;
	private ServerPlayerEventHandler localPlayerSupport;

	public ServerPlayerContainer (final ExecutorService executorService) {
		if (executorService == null) throw new IllegalArgumentException();
		this.executorService = executorService;
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
		return "Server";
	}

	@Override
	public LocalPlayerSupport getLocalPlayerSupport () {
		if (this.localPlayerSupport == null) {
			this.localPlayerSupport = new ServerPlayerEventHandler(this, this.executorService);
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
