package morrigan.player;

import java.util.Collection;

import morrigan.engines.playback.IPlaybackEngine.PlayState;

public final class PlayerFinder {

	public static Player guessActivePlayer (final Collection<Player> allPlayers) {
		for (final Player p : allPlayers) {
			if (p.getPlayState() == PlayState.PLAYING) return p;
		}

		for (final Player p : allPlayers) {
			if (p.getPlayState() == PlayState.PAUSED) return p;
		}

		for (final Player p : allPlayers) {
			if (p.getCurrentItem() != null) return p;
		}

		for (final Player p : allPlayers) {
			if (p.getCurrentList() != null) return p;
		}

		for (final Player p : allPlayers) {
			if (p.getQueue().size() > 0) return p;
		}

		return null;
	}

}
