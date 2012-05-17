package com.vaguehope.morrigan.player.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.vaguehope.morrigan.player.IPlayerAbstract;
import com.vaguehope.morrigan.player.IPlayerEventHandler;
import com.vaguehope.morrigan.player.IPlayerLocal;
import com.vaguehope.morrigan.player.PlayerRegister;

public class PlayerRegisterImpl implements PlayerRegister {

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final AtomicInteger next = new AtomicInteger(0);
	private final ConcurrentMap<Integer, IPlayerAbstract> all = new ConcurrentHashMap<Integer, IPlayerAbstract>();
	private final ConcurrentMap<Integer, Integer> localPlayerIds = new ConcurrentHashMap<Integer, Integer>();

	private static final Logger LOG = Logger.getLogger(PlayerRegisterImpl.class.getName());

	@Override
	public Collection<IPlayerAbstract> getAll () {
		checkAlive();
		return Collections.unmodifiableCollection(this.all.values());
	}

	@Override
	public IPlayerAbstract get (int i) {
		checkAlive();
		return this.all.get(Integer.valueOf(i));
	}

	@Override
	public int nextIndex () {
		checkAlive();
		return this.next.getAndIncrement();
	}

	@Override
	public void register (IPlayerAbstract p) {
		checkAlive();
		Integer i = Integer.valueOf(p.getId());
		IPlayerAbstract prev = this.all.putIfAbsent(i, p);
		if (prev != null) throw new IllegalStateException("Index " + p.getId() + " already in use by player: " + this.all.get(i).getName());
	}

	/**
	 * If we made it then it will be disposed.
	 */
	@Override
	public void unregister (IPlayerAbstract p) {
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
	public IPlayerLocal makeLocal (String name, IPlayerEventHandler eventHandler) {
		IPlayerLocal p = new Player(nextIndex(), name, eventHandler, this);
		Integer i = Integer.valueOf(p.getId());
		this.localPlayerIds.put(i, i);
		register(p);
		return p;
	}

	public void disposeLocalPlayers () {
		for (Integer i : this.localPlayerIds.keySet()) {
			IPlayerAbstract p = this.all.get(i);
			if (p != null) {
				LOG.warning("Register having to dispose of local player: " + i);
				p.dispose();
			}
		}
		this.localPlayerIds.clear();
	}

}
