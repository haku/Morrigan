package com.vaguehope.morrigan.server.boot;

import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerContainer;
import com.vaguehope.morrigan.screen.ScreenPainter.TitleProvider;

public class PlayerTitleProvider implements TitleProvider {

	private final PlayerContainer playerContainer;

	public PlayerTitleProvider (PlayerContainer playerContainer) {
		if (playerContainer == null) throw new IllegalArgumentException();
		this.playerContainer = playerContainer;
	}

	@Override
	public PlayItem getItem () {
		Player p = this.playerContainer.getPlayer();
		if (p == null) return null;
		return p.getCurrentItem();
	}

}
