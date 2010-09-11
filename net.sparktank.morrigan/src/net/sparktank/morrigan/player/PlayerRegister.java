package net.sparktank.morrigan.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private int playerN = 0;
	
	static public IPlayerLocal makePlayer (IPlayerEventHandler eventHandler) {
		synchronized (listPlayers) {
			Player player = new Player(playerN, eventHandler);
			playerN++;
			addPlayer(player);
			return player;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private List<Player> listPlayers = new ArrayList<Player>();
	
	static private void addPlayer (Player player) {
		listPlayers.add(player);
	}
	
	static public void removePlayer (IPlayerLocal player) {
		listPlayers.remove(player);
	}
	
	static public List<Player> getPlayers () {
		return Collections.unmodifiableList(listPlayers);
	}
	
	static public IPlayerLocal getPlayer (int n) {
		for (IPlayerLocal p : listPlayers) {
			if (p.getId() == n) {
				return p;
			}
		}
		throw new IllegalArgumentException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
