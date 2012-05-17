package com.vaguehope.morrigan.player;

import java.util.Collection;
import java.util.Collections;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class PlayerReaderTracker implements PlayerReader {

	private final ServiceTracker<PlayerReader, PlayerReader> playerReaderTracker;

	public PlayerReaderTracker (BundleContext context) {
		this.playerReaderTracker = new ServiceTracker<PlayerReader, PlayerReader>(context, PlayerReader.class, null);
		this.playerReaderTracker.open();
	}

	public void dispose () {
		this.playerReaderTracker.close();
	}

	@Override
	public Collection<IPlayerAbstract> getPlayers () {
		PlayerReader service = this.playerReaderTracker.getService();
		if (service == null) return Collections.<IPlayerAbstract>emptyList();
		return service.getPlayers();
	}

	@Override
	public IPlayerAbstract getPlayer (int i) {
		PlayerReader service = this.playerReaderTracker.getService();
		if (service == null) return null;
		return service.getPlayer(i);
	}

}
