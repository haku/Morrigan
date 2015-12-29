package com.vaguehope.morrigan.player.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.LocalPlayerSupport;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerRegister;

public class PlayerRegisterImpl implements PlayerRegister {

	private static final Logger LOG = Logger.getLogger(PlayerRegisterImpl.class.getName());

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final AtomicInteger next = new AtomicInteger(0);
	private final ConcurrentMap<String, Player> all = new ConcurrentHashMap<String, Player>();
	private final Set<String> localPlayerIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

	private final PlaybackEngineFactory playbackEngineFactory;
	private final ExecutorService executorService;

	public PlayerRegisterImpl (final PlaybackEngineFactory playbackEngineFactory, final ExecutorService executorService) {
		this.playbackEngineFactory = playbackEngineFactory;
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
	public Player get (final String id) {
		checkAlive();
		return this.all.get(id);
	}

	@Override
	public int nextIndex () {
		checkAlive();
		return this.next.getAndIncrement();
	}

	@Override
	public void register (final Player p) {
		checkAlive();
		Player prev = this.all.putIfAbsent(p.getId(), p);
		if (prev != null) throw new IllegalStateException("Index " + p.getId() + " already in use by player: " + prev.getName());
	}

	/**
	 * If we made it then it will be disposed.
	 */
	@Override
	public void unregister (final Player p) {
		this.all.remove(p.getId());
		this.localPlayerIds.remove(p.getId());
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
	public LocalPlayer makeLocal (final String name, final LocalPlayerSupport localPlayerSupport) {
		LocalPlayer p = new LocalPlayerImpl(String.valueOf(nextIndex()), name, localPlayerSupport, this, this.playbackEngineFactory, this.executorService);
		this.localPlayerIds.add(p.getId());
		register(p);
		return p;
	}

	@Override
	public LocalPlayer makeLocalProxy (final Player player, final LocalPlayerSupport localPlayerSupport) {
		return new LocalProxyPlayer(player, localPlayerSupport);
	}

	public void disposeLocalPlayers () {
		for (final String id : this.localPlayerIds) {
			final Player p = this.all.get(id);
			if (p != null) {
				LOG.warning("Register having to dispose of local player: " + id);
				p.dispose();
			}
		}
		this.localPlayerIds.clear();
	}

}
