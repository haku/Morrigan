package net.sparktank.morrigan.player;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	All players.
	
	static final private AtomicInteger nextPlayerN = new AtomicInteger(0);
	static final private ConcurrentHashMap<Integer, IPlayerAbstract> allPlayers = new ConcurrentHashMap<Integer, IPlayerAbstract>();
	
	static public Collection<IPlayerAbstract> getAllPlayers () {
		return Collections.unmodifiableCollection(allPlayers.values());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local players.
	
	static public IPlayerLocal makeLocalPlayer (String name, IPlayerEventHandler eventHandler) {
		int n = nextPlayerN.getAndIncrement();
		Player player = new Player(n, name, eventHandler);
		addLocalPlayer(player);
		return player;
	}
	
//	-   -   -   -   -   -   -   -   -
	
	static final private ConcurrentHashMap<Integer, IPlayerLocal> localPlayers = new ConcurrentHashMap<Integer, IPlayerLocal>();
	
	static private void addLocalPlayer (IPlayerLocal player) {
		Integer id = Integer.valueOf(player.getId());
		allPlayers.put(id, player);
		localPlayers.put(id, player);
	}
	
	static public void removeLocalPlayer (IPlayerLocal player) {
		Integer id = Integer.valueOf(player.getId());
		allPlayers.remove(id);
		localPlayers.remove(id);
	}
	
	static public Collection<IPlayerLocal> getLocalPlayers () {
		return Collections.unmodifiableCollection(localPlayers.values());
	}
	
	static public IPlayerLocal getLocalPlayer (int n) {
		IPlayerLocal p = localPlayers.get(Integer.valueOf(n));
		if (p != null) return p;
		throw new IllegalArgumentException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Remote players.
	
	static public IPlayerRemote makeRemotePlayer (String name, String remoteHost, int remotePlayerId, IPlayerEventHandler eventHandler) {
		int n = nextPlayerN.getAndIncrement();
		PlayerRemote player = new PlayerRemote(n, name, remoteHost, remotePlayerId);
		addRemotePlayer(player);
		return player;
	}
	
//	-   -   -   -   -   -   -   -   -
	
	static final private ConcurrentHashMap<Integer, IPlayerRemote> remotePlayers = new ConcurrentHashMap<Integer, IPlayerRemote>();
	
	static private void addRemotePlayer (IPlayerRemote player) {
		Integer id = Integer.valueOf(player.getId());
		allPlayers.put(id, player);
		remotePlayers.put(id, player);
	}
	
	static public void removeRemotePlayer (IPlayerRemote player) {
		Integer id = Integer.valueOf(player.getId());
		allPlayers.remove(id);
		remotePlayers.remove(id);
	}
	
	static public Collection<IPlayerRemote> getRemotePlayers () {
		return Collections.unmodifiableCollection(remotePlayers.values());
	}
	
	static public IPlayerRemote getRemotePlayer (int n) {
		IPlayerRemote p = remotePlayers.get(Integer.valueOf(n));
		if (p != null) return p;
		throw new IllegalArgumentException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
