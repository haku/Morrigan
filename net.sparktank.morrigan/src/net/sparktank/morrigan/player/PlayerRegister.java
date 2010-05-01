package net.sparktank.morrigan.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private int playerN = 0;
	
	static public Player makePlayer (IPlayerEventHandler eventHandler) {
		synchronized (listPlayers) {
			Player player = new Player(eventHandler);
			player.setId(playerN);
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
	
	static public void removePlayer (Player player) {
		listPlayers.remove(player);
	}
	
	static public List<Player> getPlayers () {
		return Collections.unmodifiableList(listPlayers);
	}
	
	static public Player getPlayer (int n) {
		for (Player p : listPlayers) {
			if (p.getId() == n) {
				return p;
			}
		}
		throw new IllegalArgumentException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
