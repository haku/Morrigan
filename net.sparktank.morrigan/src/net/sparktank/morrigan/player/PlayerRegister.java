package net.sparktank.morrigan.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private int playerN = 0;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	All players.
	
	static private List<IPlayerAbstract> listAllPlayers = new ArrayList<IPlayerAbstract>();
	
	static public List<IPlayerAbstract> getAllPlayers () {
		return Collections.unmodifiableList(listAllPlayers);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local players.
	
	static public IPlayerLocal makeLocalPlayer (IPlayerEventHandler eventHandler) {
		synchronized (listLocalPlayers) {
			Player player = new Player(playerN, eventHandler);
			playerN++;
			addLocalPlayer(player);
			return player;
		}
	}
	
//	-   -   -   -   -   -   -   -   -
	
	static private List<IPlayerLocal> listLocalPlayers = new ArrayList<IPlayerLocal>();
	
	static private void addLocalPlayer (IPlayerLocal player) {
		listLocalPlayers.add(player);
		listAllPlayers.add(player);
	}
	
	static public void removeLocalPlayer (IPlayerLocal player) {
		listLocalPlayers.remove(player);
		listAllPlayers.remove(player);
	}
	
	static public List<IPlayerLocal> getLocalPlayers () {
		return Collections.unmodifiableList(listLocalPlayers);
	}
	
	static public IPlayerLocal getLocalPlayer (int n) {
		for (IPlayerLocal p : listLocalPlayers) {
			if (p.getId() == n) {
				return p;
			}
		}
		throw new IllegalArgumentException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Remote players.
	
	static public IPlayerRemote makeRemotePlayer (String remoteHost, int remotePlayerId, IPlayerEventHandler eventHandler) {
		synchronized (listLocalPlayers) {
			PlayerRemote player = new PlayerRemote(playerN, remoteHost, remotePlayerId);
			playerN++;
			addRemotePlayer(player);
			return player;
		}
	}
	
//	-   -   -   -   -   -   -   -   -
	
	static private List<IPlayerRemote> listRemotePlayers = new ArrayList<IPlayerRemote>();
	
	static private void addRemotePlayer (IPlayerRemote player) {
		listRemotePlayers.add(player);
		listAllPlayers.add(player);
	}
	
	static public void removeRemotePlayer (IPlayerRemote player) {
		listRemotePlayers.remove(player);
		listAllPlayers.remove(player);
	}
	
	static public List<IPlayerRemote> getRemotePlayers () {
		return Collections.unmodifiableList(listRemotePlayers);
	}
	
	static public IPlayerRemote getRemotePlayer (int n) {
		for (IPlayerRemote p : listRemotePlayers) {
			if (p.getId() == n) {
				return p;
			}
		}
		throw new IllegalArgumentException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
