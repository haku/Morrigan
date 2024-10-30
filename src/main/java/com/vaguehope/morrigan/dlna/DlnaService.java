package com.vaguehope.morrigan.dlna;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.resource.IconResource;
import org.jupnp.model.resource.Resource;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.ServletStreamServerImpl;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.ServletContainerAdapter;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.Args;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.dlna.content.MediaFileLocator;
import com.vaguehope.morrigan.dlna.content.MediaServerDeviceFactory;
import com.vaguehope.morrigan.dlna.extcd.ContentDirectoryHolder;
import com.vaguehope.morrigan.dlna.httpserver.MediaServer;
import com.vaguehope.morrigan.dlna.players.DlnaPlayingParamsFactory;
import com.vaguehope.morrigan.dlna.players.PlayerControlBridgeFactory;
import com.vaguehope.morrigan.dlna.players.PlayerHolder;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.PlayerStateStorage;
import com.vaguehope.morrigan.server.ServerConfig;
import com.vaguehope.morrigan.util.DaemonThreadFactory;

public class DlnaService {

	private static final int BG_THREADS = 3;
	private static final Logger LOG = LoggerFactory.getLogger(DlnaService.class);

	private final Args args;
	private final Config config;
	private final ServerConfig serverConfig;
	private final MediaFactory mediaFactory;
	private final PlayerRegister playerRegister;

	private final InetAddress bindAddress;
	private final Map<String, Resource<?>> registryPathToRes = new HashMap<>();

	private MediaServer mediaServer;
	private PlayerHolder playerHolder;
	private MyUpnpService upnpService;
	private ContentDirectoryHolder contentDirectoryHolder;
	private ScheduledExecutorService scheduledExecutor;

	public DlnaService(final Args args, final Config config, final ServerConfig serverConfig, final MediaFactory mediaFactory, final PlayerRegister playerRegister) throws IOException {
		this.args = args;
		this.config = config;
		this.serverConfig = serverConfig;
		this.mediaFactory = mediaFactory;
		this.playerRegister = playerRegister;

		this.bindAddress = this.serverConfig.getBindAddress("DLNA");
		if (this.bindAddress == null) throw new IllegalStateException("Failed to find bind address.");

		final Icon icon = UpnpHelper.createDeviceIcon();
		final IconResource iconResource = new IconResource(icon.getUri(), icon);
		this.registryPathToRes.put("/icon.png", iconResource);
	}

	public void start() throws IOException, ValidationException {
		final SystemId systemId = new SystemId(this.config.getSystemIdFile());
		this.scheduledExecutor = Executors.newScheduledThreadPool(BG_THREADS, new DaemonThreadFactory("dlna"));

		final MediaFileLocator mediaFileLocator = new MediaFileLocator(this.mediaFactory);
		this.mediaServer = new MediaServer(mediaFileLocator, this.bindAddress);
		this.mediaServer.start();
		final DlnaPlayingParamsFactory dlnaPlayingParamsFactory = new DlnaPlayingParamsFactory(mediaFileLocator, this.mediaServer);

		this.upnpService = new MyUpnpService(new MyUpnpServiceConfiguration());

		// watcher needs to be ready BEFORE upnpService starts, otherwise early discovery msgs may be missed.
		this.playerHolder = new PlayerHolder(
				this.playerRegister,
				this.upnpService,
				dlnaPlayingParamsFactory,
				new PlayerStateStorage(this.mediaFactory, this.scheduledExecutor, this.config),
				this.config,
				this.scheduledExecutor);
		this.contentDirectoryHolder = new ContentDirectoryHolder(this.upnpService, this.mediaFactory, this.config);
		this.upnpService.setWatcher(new DeviceWatcher(this.playerHolder, this.contentDirectoryHolder));

		this.upnpService.startup();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				DlnaService.this.upnpService.shutdown();
			}
		});

		this.upnpService.getRegistry().addDevice(new MediaServerDeviceFactory(
				systemId,
				this.mediaFactory,
				this.mediaServer,
				mediaFileLocator
				).getDevice());

		if (this.args.isDlnaPlayerControl()) {
			// TODO replace with event listener inside playerreg to dynamically reg/unreg
			// which is needed for non-local players (this assumes local players are static).
			for (final Player p : this.playerRegister.getAll()) {
				if (!(p instanceof LocalPlayer)) continue;
				final LocalDevice device = PlayerControlBridgeFactory.makeMediaRendererDevice(systemId, p, dlnaPlayingParamsFactory, this.scheduledExecutor);
				this.upnpService.getRegistry().addDevice(device);
				p.addOnDisposeListener(() -> this.upnpService.getRegistry().removeDevice(device));
				LOG.info("Made DLNA contol proxy for player: " + p);
			}
		}

		this.upnpService.getControlPoint().search();

		// Periodic rescan to catch missed devices.
		this.scheduledExecutor.scheduleWithFixedDelay(() -> {
			this.upnpService.getControlPoint().search();
			if (this.args.isVerboseLog()) LOG.info("Scanning for devices.");
		}, 1, 3, TimeUnit.MINUTES);

		LOG.info("DLNA started.");
	}

	public UpnpService getUpnpService() {
		return this.upnpService;
	}

	public PlayerHolder getPlayerHolder() {
		return this.playerHolder;
	}

	private class MyUpnpService extends UpnpServiceImpl {

		private RegistryListener watcher;

		private MyUpnpService(final UpnpServiceConfiguration configuration) {
			super(configuration);
		}

		void setWatcher(RegistryListener watcher) {
			this.watcher = watcher;
		}

		@Override
		protected Registry createRegistry (final ProtocolFactory pf) {
			if (this.watcher == null) throw new IllegalStateException();

			final RegistryImplWithOverrides r = new RegistryImplWithOverrides(this, DlnaService.this.registryPathToRes);
			// watcher added here so that it is listening from Before the UPNP service starts so no msgs are missed.
			r.addListener(this.watcher);
			return r;
		}

	}

	private class MyUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

		@Override
		protected NetworkAddressFactory createNetworkAddressFactory(final int streamListenPort, final int multicastResponsePort) {
			return new MyNetworkAddressFactory(streamListenPort, multicastResponsePort);
		}

		private final ServletContainerAdapter jettyAdaptor = new MyJettyServletContainer();

		// Workaround for https://github.com/jupnp/jupnp/issues/225
		// TODO remove this override once it is fixed.
		@Override
		public StreamServer createStreamServer(final NetworkAddressFactory networkAddressFactory) {
			return new ServletStreamServerImpl(new ServletStreamServerConfigurationImpl(this.jettyAdaptor, networkAddressFactory.getStreamListenPort()));
		}

		@Override
		protected ExecutorService createDefaultExecutorService() {
			return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>(), new DaemonThreadFactory("upnp"));
		}

	}

	private class MyNetworkAddressFactory extends NetworkAddressFactoryImpl {

		private MyNetworkAddressFactory(final int streamListenPort, final int multicastResponsePort) throws InitializationException {
			super(streamListenPort, multicastResponsePort);
		}

		@Override
		protected boolean isUsableAddress(final NetworkInterface iface, final InetAddress address) {
			return DlnaService.this.bindAddress.equals(address);
		}

	}

	private class MyJettyServletContainer implements ServletContainerAdapter {

		protected Server server;

		public MyJettyServletContainer() {
			resetServer();
		}

		@Override
		public void setExecutorService(final ExecutorService executorService) {
			// not needed.
		}

		@SuppressWarnings("resource")
		@Override
		public int addConnector(final String host, final int port) throws IOException {
			final ServerConnector connector = new ServerConnector(this.server);
			connector.setHost(host);
			connector.setPort(port);
			connector.open();
			this.server.addConnector(connector);
			return connector.getLocalPort();
		}

		@Override
		public void registerServlet(final String contextPath, final Servlet servlet) {
			if (this.server.getHandler() != null) {
				return;
			}
			final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
			if (contextPath != null && !contextPath.isEmpty()) {
				servletHandler.setContextPath(contextPath);
			}
			final ServletHolder s = new ServletHolder(servlet);
			servletHandler.addServlet(s, "/*");
			this.server.setHandler(servletHandler);
		}

		@Override
		public synchronized void startIfNotRunning() {
			if (!this.server.isStarted() && !this.server.isStarting()) {
				try {
					this.server.start();
				}
				catch (final Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		public synchronized void stopIfRunning() {
			if (!this.server.isStopped() && !this.server.isStopping()) {
				try {
					this.server.stop();
				}
				catch (final Exception e) {
					throw new RuntimeException(e);
				}
				finally {
					resetServer();
				}
			}
		}

		protected void resetServer() {
			this.server = new Server();
		}

	}

}
