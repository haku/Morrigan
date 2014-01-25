package com.vaguehope.morrigan.player.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerEventHandler;
import com.vaguehope.morrigan.player.PlayerRegister;

public class PlayerRegisterImpl implements PlayerRegister {

	private static final Logger LOG = Logger.getLogger(PlayerRegisterImpl.class.getName());

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final AtomicInteger next = new AtomicInteger(0);
	private final ConcurrentMap<Integer, Player> all = new ConcurrentHashMap<Integer, Player>();
	private final ConcurrentMap<Integer, Integer> localPlayerIds = new ConcurrentHashMap<Integer, Integer>();

	private final PlaybackEngineFactory playbackEngineFactory;
	private final MediaFactory mediaFactory;
	private final ExecutorService executorService;

	public PlayerRegisterImpl (final PlaybackEngineFactory playbackEngineFactory, final MediaFactory mediaFactory, final ExecutorService executorService) {
		this.playbackEngineFactory = playbackEngineFactory;
		this.mediaFactory = mediaFactory;
		this.executorService = executorService;
	}

	@Override
	public Collection<Player> getAll () {
		checkAlive();
		final List<Player> l = new ArrayList<Player>(this.all.values());
		Collections.sort(l, PlayerSorter.ID);
		return Collections.unmodifiableCollection(l);
	}

	@Override
	public Player get (final int i) {
		checkAlive();
		return this.all.get(Integer.valueOf(i));
	}

	@Override
	public int nextIndex () {
		checkAlive();
		return this.next.getAndIncrement();
	}

	@Override
	public void register (final Player p) {
		checkAlive();
		Integer i = Integer.valueOf(p.getId());
		Player prev = this.all.putIfAbsent(i, p);
		if (prev != null) throw new IllegalStateException("Index " + p.getId() + " already in use by player: " + this.all.get(i).getName());
	}

	/**
	 * If we made it then it will be disposed.
	 */
	@Override
	public void unregister (final Player p) {
		Integer i = Integer.valueOf(p.getId());
		this.all.remove(i);
		this.localPlayerIds.remove(i);
		if (this.all.containsValue(p)) throw new IllegalStateException("Player " + p.getName() + " was registered under a different ID and could not be unregistered.");
	}

	/**
	 * Disable this registry.
	 * Dispose all the players we made that are still registered.
	 */
	public void dispose () {
		this.alive.set(false);
		disposeLocalPlayers();
		this.all.clear();
	}

	private void checkAlive () {
		if (!this.alive.get()) throw new IllegalStateException("PlayerRegister instance is dead.");
	}

	@Override
	public LocalPlayer makeLocal (final String name, final PlayerEventHandler eventHandler) {
		LocalPlayer p = new LocalPlayerImpl(nextIndex(), name, eventHandler, this, this.playbackEngineFactory, this.mediaFactory, this.executorService);
		Integer i = Integer.valueOf(p.getId());
		this.localPlayerIds.put(i, i);
		register(p);
		return p;
	}

	public void disposeLocalPlayers () {
		for (Integer i : this.localPlayerIds.keySet()) {
			Player p = this.all.get(i);
			if (p != null) {
				LOG.warning("Register having to dispose of local player: " + i);
				p.dispose();
			}
		}
		this.localPlayerIds.clear();
	}

}
