package com.vaguehope.morrigan.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.contentproxy.ContentProxy;

public class PlayerRegisterImpl implements PlayerRegister, PlayerReader {

	private static final Logger LOG = Logger.getLogger(PlayerRegisterImpl.class.getName());

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final ConcurrentMap<String, AtomicInteger> nexts = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Player> all = new ConcurrentHashMap<>();
	private final Set<String> localPlayerIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

	private final PlayerStateStorage stateStorage;
	private final MediaFactory mediaFactory;
	private final Config config;
	private final ContentProxy contentProxy;
	private final ScheduledExecutorService schEx;

	public PlayerRegisterImpl(
			final PlayerStateStorage stateStorage,
			final MediaFactory mediaFactory,
			final Config config,
			final ContentProxy contentProxy,
			final ScheduledExecutorService schEx) {
		this.stateStorage = stateStorage;
		this.mediaFactory = mediaFactory;
		this.config = config;
		this.contentProxy = contentProxy;
		this.schEx = schEx;
	}

	@Override
	public Collection<Player> getPlayers() {
		return getAll();
	}

	@Override
	public Player getPlayer(String id) {
		return get(id);
	}

	@Override
	public Collection<Player> getAll () {
		checkAlive();
		final List<Player> l = new ArrayList<>(this.all.values());
		Collections.sort(l, PlayerSorter.ID);
		return Collections.unmodifiableCollection(l);
	}

	@Override
	public Player get (final String id) {
		checkAlive();
		return this.all.get(id);
	}

	@Override
	public String nextIndex (final String prefix) {
		checkAlive();

		AtomicInteger next = this.nexts.get(prefix);
		if (next == null) {
			final AtomicInteger newNext = new AtomicInteger(0);
			final AtomicInteger prevNext = this.nexts.putIfAbsent(prefix, newNext);
			next = prevNext != null ? prevNext : newNext;
		}

		return prefix + next.getAndIncrement();
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
	public Player makeLocal (final String prefix, final String name, PlaybackEngineFactory playbackEngineFactory) {
		return make(nextIndex(prefix), name, playbackEngineFactory);
	}

	@Override
	public Player make(final String id, final String name, PlaybackEngineFactory playbackEngineFactory) {
		final LocalPlayer p = new LocalPlayer(
				id,
				name,
				this,
				this.mediaFactory,
				playbackEngineFactory,
				this.contentProxy,
				this.schEx,
				this.stateStorage,
				this.config);
		this.stateStorage.requestReadState(p);
		this.localPlayerIds.add(p.getId());
		register(p);
		return p;
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
