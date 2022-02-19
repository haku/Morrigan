package com.vaguehope.morrigan.player.internal;

import java.util.Comparator;

import com.vaguehope.morrigan.player.Player;

public enum PlayerSorter implements Comparator<Player> {
	ID {
		@Override
		public int compare (final Player p1, final Player p2) {
			return p1.getId().compareTo(p2.getId());
		}
	};

	@Override
	public abstract int compare (Player p1, Player p2);

}
