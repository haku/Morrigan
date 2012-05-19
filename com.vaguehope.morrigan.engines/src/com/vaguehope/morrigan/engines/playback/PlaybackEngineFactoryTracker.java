package com.vaguehope.morrigan.engines.playback;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class PlaybackEngineFactoryTracker implements PlaybackEngineFactory {

	private static final Logger LOG = Logger.getLogger(PlaybackEngineFactoryTracker.class.getName());

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final ServiceTracker<PlaybackEngineFactory, PlaybackEngineFactory> playerReaderTracker;

	public PlaybackEngineFactoryTracker (BundleContext context) {
		this.playerReaderTracker = new ServiceTracker<PlaybackEngineFactory, PlaybackEngineFactory>(context, PlaybackEngineFactory.class, null);
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
	public IPlaybackEngine newPlaybackEngine () {
		checkAlive();
		PlaybackEngineFactory[] services = this.playerReaderTracker.getServices(new PlaybackEngineFactory[]{});
		if (services == null || services.length < 1) return null;
		for (PlaybackEngineFactory service : services) {
			try {
				IPlaybackEngine engine = service.newPlaybackEngine();
				if (engine != null) return engine;
			}
			catch (Exception e) {
				LOG.log(Level.WARNING, "Exception while trying to create new playback engine instance.", e);
			}
		}
		return null;
	}

}
