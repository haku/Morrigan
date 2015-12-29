package com.vaguehope.morrigan.player;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class PlayerReaderTracker implements PlayerReader {

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final ServiceTracker<PlayerReader, PlayerReader> playerReaderTracker;

	public PlayerReaderTracker (final BundleContext context) {
		this.playerReaderTracker = new ServiceTracker<PlayerReader, PlayerReader>(context, PlayerReader.class, null);
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
	public Collection<Player> getPlayers () {
		checkAlive();
		PlayerReader service = this.playerReaderTracker.getService();
		if (service == null) return Collections.<Player>emptyList();
		return service.getPlayers();
	}

	@Override
	public Player getPlayer (final String id) {
		checkAlive();
		PlayerReader service = this.playerReaderTracker.getService();
		if (service == null) return null;
		return service.getPlayer(id);
	}

}
