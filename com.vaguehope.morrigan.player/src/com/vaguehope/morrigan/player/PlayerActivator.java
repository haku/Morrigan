package com.vaguehope.morrigan.player;

import java.util.Collection;
import java.util.Collections;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactoryTracker;
import com.vaguehope.morrigan.model.media.MediaFactoryTracker;
import com.vaguehope.morrigan.player.internal.PlayerRegisterImpl;

public final class PlayerActivator implements BundleActivator {

	protected PlayerRegisterImpl playerRegister;
	private PlaybackEngineFactoryTracker playbackEngineFactoryTracker;
	private MediaFactoryTracker mediaFactoryTracker;

	@Override
	public void start (BundleContext context) throws Exception {
		this.playbackEngineFactoryTracker = new PlaybackEngineFactoryTracker(context);
		this.mediaFactoryTracker = new MediaFactoryTracker(context);
		this.playerRegister = new PlayerRegisterImpl(this.playbackEngineFactoryTracker, this.mediaFactoryTracker);

		startPlayerContainerListener(context);
		context.registerService(PlayerReader.class, this.playerListener, null);
		context.registerService(PlayerRegister.class, this.playerRegister, null);
	}

	@Override
	public void stop (BundleContext context) throws Exception {
		this.playerRegister.dispose();
		this.mediaFactoryTracker.dispose();
		this.playbackEngineFactoryTracker.dispose();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	PlayerContainers.

	private final static String FILTER = "(objectclass=" + PlayerContainer.class.getName() + ")";

	private void startPlayerContainerListener (final BundleContext context) {
		ServiceListener playerContainerSl = new ServiceListener() {
			@Override
			public void serviceChanged (ServiceEvent ev) {
				switch (ev.getType()) {
					case ServiceEvent.REGISTERED:
						fillPlayerContainer((PlayerContainer) context.getService(ev.getServiceReference()));
						break;
					case ServiceEvent.UNREGISTERING:
						emptyPlayerContainer((PlayerContainer) context.getService(ev.getServiceReference()));
						break;
				}
			}
		};

		try {
			context.addServiceListener(playerContainerSl, FILTER);
			Collection<ServiceReference<PlayerContainer>> refs = context.getServiceReferences(PlayerContainer.class, FILTER);
			for (ServiceReference<PlayerContainer> ref : refs) {
				fillPlayerContainer(context.getService(ref));
			}
		}
		catch (InvalidSyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	protected void fillPlayerContainer (PlayerContainer container) {
		container.setPlayer(this.playerRegister.makeLocal(container.getName(), container.getEventHandler()));
	}

	protected void emptyPlayerContainer (PlayerContainer container) {
		this.playerRegister.unregister(container.getPlayer());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	PlayerListeners.

	private final PlayerReader playerListener = new PlayerReader() {

		@Override
		public Collection<Player> getPlayers () {
			PlayerRegisterImpl r = PlayerActivator.this.playerRegister;
			if (r == null) return Collections.<Player> emptyList();
			return r.getAll();
		}

		@Override
		public Player getPlayer (int i) {
			PlayerRegisterImpl r = PlayerActivator.this.playerRegister;
			if (r == null) return null;
			return r.get(i);
		}

	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
