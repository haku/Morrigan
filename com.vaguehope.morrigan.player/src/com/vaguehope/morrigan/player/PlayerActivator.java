package com.vaguehope.morrigan.player;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactoryTracker;
import com.vaguehope.morrigan.model.media.MediaFactoryTracker;
import com.vaguehope.morrigan.player.internal.PlayerRegisterImpl;
import com.vaguehope.morrigan.util.DaemonThreadFactory;

public final class PlayerActivator implements BundleActivator {

	private static final Logger logger = Logger.getLogger(PlayerActivator.class.getName());

	protected PlayerRegisterImpl playerRegister;
	private PlaybackEngineFactoryTracker playbackEngineFactoryTracker;
	private MediaFactoryTracker mediaFactoryTracker;
	private ScheduledThreadPoolExecutor scheduledExecutor;

	@Override
	public void start (final BundleContext context) throws Exception {
		this.playbackEngineFactoryTracker = new PlaybackEngineFactoryTracker(context);
		this.mediaFactoryTracker = new MediaFactoryTracker(context);

		this.scheduledExecutor = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory("player"));
		this.scheduledExecutor.setKeepAliveTime(1, TimeUnit.MINUTES);
		this.scheduledExecutor.allowCoreThreadTimeOut(true);

		this.playerRegister = new PlayerRegisterImpl(this.playbackEngineFactoryTracker,
				new PlayerStateStorage(this.mediaFactoryTracker, this.scheduledExecutor), this.scheduledExecutor);

		startPlayerContainerListener(context);
		context.registerService(PlayerReader.class, this.playerListener, null);
		context.registerService(PlayerRegister.class, this.playerRegister, null);
	}

	@Override
	public void stop (final BundleContext context) throws Exception {
		if (this.playerRegister != null) {
			this.playerRegister.dispose();
			this.playerRegister = null;
		}

		if (this.scheduledExecutor != null) {
			this.scheduledExecutor.shutdownNow();
			this.scheduledExecutor = null;
		}

		if (this.playbackEngineFactoryTracker != null) {
			this.playbackEngineFactoryTracker.dispose();
			this.playbackEngineFactoryTracker = null;
		}

		if (this.mediaFactoryTracker != null) {
			this.mediaFactoryTracker.dispose();
			this.mediaFactoryTracker = null;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	PlayerContainers.

	private static final String FILTER = "(objectclass=" + PlayerContainer.class.getName() + ")";

	private void startPlayerContainerListener (final BundleContext context) {
		ServiceListener playerContainerSl = new ServiceListener() {
			@Override
			public void serviceChanged (final ServiceEvent ev) {
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

	protected void fillPlayerContainer (final PlayerContainer container) {
		try {
			container.setPlayer(this.playerRegister.makeLocal(container.getPrefix(), container.getName(), container.getLocalPlayerSupport()));
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Failed to inject player object into container '" + container + "'.", e);
		}
	}

	protected void emptyPlayerContainer (final PlayerContainer container) {
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
		public Player getPlayer (final String id) {
			PlayerRegisterImpl r = PlayerActivator.this.playerRegister;
			if (r == null) return null;
			return r.get(id);
		}

	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
