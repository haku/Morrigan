package com.vaguehope.morrigan.player;

import java.util.Collection;

public interface PlayerReader {

	/**
	 * Returns all players.
	 * Will return at least empty set.
	 */
	Collection<Player> getPlayers ();

	/**
	 * Returns player with ID id.
	 * Will return null if ID not found.
	 */
	Player getPlayer (String id);

}
