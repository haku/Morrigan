package com.vaguehope.morrigan.player;

import java.util.Collection;

public interface PlayerReader {

	/**
	 * Returns all players.
	 * Will return at least empty set.
	 */
	Collection<IPlayerAbstract> getPlayers ();

	/**
	 * Returns player with ID i.
	 * Will return null if ID not found.
	 */
	IPlayerAbstract getPlayer (int i);

}
