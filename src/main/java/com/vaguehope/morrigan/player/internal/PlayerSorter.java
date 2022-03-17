package com.vaguehope.morrigan.player.internal;

import java.util.Comparator;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.player.Player;

public enum PlayerSorter implements Comparator<Player> {
	ID {
		@Override
		public int compare (final Player p1, final Player p2) {
			return p1.getId().compareTo(p2.getId());
		}
	},
	STATE {
		@Override
		public int compare(final Player p1, final Player p2) {
			final PlayState s1 = p1.getPlayState();
			final PlayState s2 = p2.getPlayState();
			if (s1 == s2) return ID.compare(p1, p2);
			return s1.getSortPriority() < s2.getSortPriority() ? -1 : 1;
		}

	};

	@Override
	public abstract int compare (Player p1, Player p2);

}
