package morrigan.player.test;

import java.util.Collection;
import java.util.Collections;

import morrigan.player.Player;
import morrigan.player.PlayerReader;

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
