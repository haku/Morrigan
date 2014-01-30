package com.vaguehope.morrigan.player;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class PlayerRegisterTracker implements PlayerRegister {

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final ServiceTracker<PlayerRegister, PlayerRegister> tracker;

	public PlayerRegisterTracker (final BundleContext context) {
		this.tracker = new ServiceTracker<PlayerRegister, PlayerRegister>(context, PlayerRegister.class, null);
		this.tracker.open();
	}

	public void dispose () {
		this.alive.set(false);
		this.tracker.close();
	}

	private void checkAlive () {
		if (!this.alive.get()) throw new IllegalStateException(this.getClass().getName() + " is disposed.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private PlayerRegister getServiceOptional () {
		checkAlive();
		PlayerRegister service = this.tracker.getService();
		return service;
	}

	private PlayerRegister getService () {
		PlayerRegister service = getServiceOptional();
		if (service == null) throw new IllegalStateException("PlayerRegister service not available.");
		return service;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public int nextIndex () {
		return getService().nextIndex();
	}

	@Override
	public void register (final Player target) {
		getService().register(target);
	}

	@Override
	public void unregister (final Player target) {
		getService().unregister(target);
	}

	@Override
	public Collection<Player> getAll () {
		PlayerRegister service = getServiceOptional();
		return (service == null) ? Collections.<Player>emptyList() : service.getAll();
	}

	@Override
	public Player get (final int i) {
		PlayerRegister service = getServiceOptional();
		return (service == null) ? null : service.get(i);
	}

	@Override
	public LocalPlayer makeLocal (final String name, final LocalPlayerSupport localPlayerSupport) {
		return getService().makeLocal(name, localPlayerSupport);
	}

	@Override
	public LocalPlayer makeLocalProxy (final Player player, final LocalPlayerSupport localPlayerSupport) {
		return getService().makeLocalProxy(player, localPlayerSupport);
	}
	@Override
	public String toString () {
		if (!this.alive.get()) return "PlayerRegisterTracker(dead)";
		return "PlayerRegisterTracker(" + getServiceOptional() + ")";
	}

}
