package com.vaguehope.morrigan.engines.hotkey;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class HotkeyEngineFactoryTracker implements HotkeyEngineFactory {

	private static final Logger LOG = Logger.getLogger(HotkeyEngineFactoryTracker.class.getName());

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final ServiceTracker<HotkeyEngineFactory, HotkeyEngineFactory> playerReaderTracker;

	public HotkeyEngineFactoryTracker (BundleContext context) {
		this.playerReaderTracker = new ServiceTracker<HotkeyEngineFactory, HotkeyEngineFactory>(context, HotkeyEngineFactory.class, null);
		this.playerReaderTracker.open();
	}

	public void dispose () {
		this.alive.set(false);
		this.playerReaderTracker.close();
	}

	private void checkAlive () {
		if (!this.alive.get()) throw new IllegalStateException(this.getClass().getName() + " is disposed.");
	}

	@Override
	public boolean canMakeHotkeyEngine () {
		return !this.playerReaderTracker.isEmpty();
	}

	@Override
	public IHotkeyEngine newHotkeyEngine () {
		checkAlive();
		HotkeyEngineFactory[] services = this.playerReaderTracker.getServices(new HotkeyEngineFactory[]{});
		if (services == null || services.length < 1) return null;
		for (HotkeyEngineFactory service : services) {
			try {
				IHotkeyEngine engine = service.newHotkeyEngine();
				if (engine != null) return engine;
			}
			catch (Exception e) {
				LOG.log(Level.WARNING, "Exception while trying to create new playback engine instance.", e);
			}
		}
		return null;
	}

}
