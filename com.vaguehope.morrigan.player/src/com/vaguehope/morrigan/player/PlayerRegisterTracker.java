package com.vaguehope.morrigan.player;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class PlayerRegisterTracker implements PlayerRegister {

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final ServiceTracker<PlayerRegister, PlayerRegister> tracker;

	public PlayerRegisterTracker (BundleContext context) {
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
	public void register (IPlayerAbstract target) {
		getService().register(target);
	}

	@Override
	public void unregister (IPlayerAbstract target) {
		getService().unregister(target);
	}

	@Override
	public Collection<IPlayerAbstract> getAll () {
		PlayerRegister service = getServiceOptional();
		return (service == null) ? Collections.<IPlayerAbstract>emptyList() : service.getAll();
	}

	@Override
	public IPlayerAbstract get (int i) {
		PlayerRegister service = getServiceOptional();
		return (service == null) ? null : service.get(i);
	}

	@Override
	public IPlayerLocal makeLocal (String name, IPlayerEventHandler eventHandler) {
		return getService().makeLocal(name, eventHandler);
	}

}
