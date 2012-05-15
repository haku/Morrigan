package com.vaguehope.morrigan.player.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaguehope.morrigan.model.Register;
import com.vaguehope.morrigan.player.IPlayerAbstract;

public class PlayerRegister implements Register<IPlayerAbstract> {

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final AtomicInteger next = new AtomicInteger(0);
	private final ConcurrentHashMap<Integer, IPlayerAbstract> all = new ConcurrentHashMap<Integer, IPlayerAbstract>();

	public Collection<IPlayerAbstract> getAll () {
		checkAlive();
		return Collections.unmodifiableCollection(this.all.values());
	}

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

	@Override
	public void unregister (IPlayerAbstract p) {
		this.all.remove(Integer.valueOf(p.getId()));
		if (this.all.containsValue(p)) throw new IllegalStateException("Player " + p.getName() + " was registered under a different ID and could not be unregistered.");
	}

	public void dispose () {
		this.alive.set(false);
	}

	private void checkAlive () {
		if (!this.alive.get()) throw new IllegalStateException("PlayerRegister instance is dead.");
	}

}
