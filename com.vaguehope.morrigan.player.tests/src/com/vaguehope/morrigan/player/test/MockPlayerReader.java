package com.vaguehope.morrigan.player.test;

import java.util.Collection;
import java.util.Collections;

import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerReader;

public class MockPlayerReader implements PlayerReader {

	@Override
	public Collection<Player> getPlayers () {
		return Collections.emptyList();
	}

	@Override
	public Player getPlayer (final String id) {
		throw new UnsupportedOperationException("Not implemented.");
	}

}
