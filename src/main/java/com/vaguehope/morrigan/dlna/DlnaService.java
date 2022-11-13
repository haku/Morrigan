package com.vaguehope.morrigan.dlna;

import java.io.IOException;
import java.io.InputStream;
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

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.resource.IconResource;
import org.fourthline.cling.model.resource.Resource;
import org.fourthline.cling.protocol.ProtocolFactory;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.transport.impl.NetworkAddressFactoryImpl;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.dlna.content.MediaFileLocator;
import com.vaguehope.morrigan.dlna.content.MediaServerDeviceFactory;
import com.vaguehope.morrigan.dlna.extcd.ContentDirectoryHolder;
import com.vaguehope.morrigan.dlna.httpserver.MediaServer;
import com.vaguehope.morrigan.dlna.players.PlayerHolder;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.PlayerStateStorage;
import com.vaguehope.morrigan.server.ServerConfig;
import com.vaguehope.morrigan.util.DaemonThreadFactory;

public class DlnaService {

	private static final int BG_THREADS = 3;
	private static final Logger LOG = LoggerFactory.getLogger(DlnaService.class);

	private final Config config;
	private final ServerConfig serverConfig;
	private final MediaFactory mediaFactory;
	private final PlayerRegister playerRegister;

	private final InetAddress bindAddress;
	private final Map<String, Resource<?>> registryPathToRes = new HashMap<>();

	private MediaServer mediaServer;
	private PlayerHolder playerHolder;
	private UpnpService upnpService;
	private ContentDirectoryHolder contentDirectoryHolder;
	private ScheduledExecutorService scheduledExecutor;

	public DlnaService(final Config config, final ServerConfig serverConfig, final MediaFactory mediaFactory, final PlayerRegister playerRegister) throws IOException {
		this.config = config;
		this.serverConfig = serverConfig;
		this.mediaFactory = mediaFactory;
		this.playerRegister = playerRegister;

		this.bindAddress = this.serverConfig.getBindAddress("DLNA");
		if (this.bindAddress == null) throw new IllegalStateException("Failed to find bind address.");

		final Icon icon = createDeviceIcon();
		final IconResource iconResource = new IconResource(icon.getUri(), icon);
		this.registryPathToRes.put("/icon.png", iconResource);
	}

	public void start() throws IOException, ValidationException {
		this.scheduledExecutor = Executors.newScheduledThreadPool(BG_THREADS, new DaemonThreadFactory("dlna"));

		final MediaFileLocator mediaFileLocator = new MediaFileLocator(this.mediaFactory);
		this.mediaServer = new MediaServer(mediaFileLocator, this.bindAddress);
		this.mediaServer.start();

		this.upnpService = new MyUpnpService(new MyUpnpServiceConfiguration());
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				DlnaService.this.upnpService.shutdown();
			}
		});

		this.playerHolder = new PlayerHolder(
				this.playerRegister,
				this.upnpService.getControlPoint(),
				this.mediaServer,
				mediaFileLocator,
				new PlayerStateStorage(this.mediaFactory, this.scheduledExecutor, this.config),
				this.config,
				this.scheduledExecutor);

		this.upnpService.getRegistry().addDevice(new MediaServerDeviceFactory(
				InetAddress.getLocalHost().getHostName(),
				this.mediaFactory,
				this.mediaServer,
				mediaFileLocator
				).getDevice());

		this.contentDirectoryHolder = new ContentDirectoryHolder(this.upnpService.getControlPoint(), this.mediaFactory, this.config);

		this.upnpService.getRegistry().addListener(new DeviceWatcher(this.playerHolder, this.contentDirectoryHolder));
		this.upnpService.getControlPoint().search();

		LOG.info("DLNA started.");
	}

	public UpnpService getUpnpService() {
		return this.upnpService;
	}

	public PlayerHolder getPlayerHolder() {
		return this.playerHolder;
	}

	private class MyUpnpService extends UpnpServiceImpl {

		private MyUpnpService(final UpnpServiceConfiguration configuration) {
			super(configuration);
		}

		@Override
		protected Registry createRegistry (final ProtocolFactory pf) {
			return new RegistryImplWithOverrides(this, DlnaService.this.registryPathToRes);
		}

	}

	private class MyUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

		@Override
		protected NetworkAddressFactory createNetworkAddressFactory(final int streamListenPort) {
			return new MyNetworkAddressFactory(streamListenPort);
		}

		@Override
		protected ExecutorService createDefaultExecutorService() {
			return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>(), new DaemonThreadFactory("upnp"));
		}

	}

	private class MyNetworkAddressFactory extends NetworkAddressFactoryImpl {

		private MyNetworkAddressFactory(final int streamListenPort) throws InitializationException {
			super(streamListenPort);
		}

		@Override
		protected boolean isUsableAddress(final NetworkInterface iface, final InetAddress address) {
			return DlnaService.this.bindAddress.equals(address);
		}

	}

	public static Icon createDeviceIcon () throws IOException {
		final InputStream res = DlnaService.class.getResourceAsStream("/icon.png");
		try {
			if (res == null) throw new IllegalStateException("Icon not found.");
			final Icon icon = new Icon("image/png", 48, 48, 8, "icon.png", res);
			icon.validate();
			return icon;
		}
		finally {
			if (res != null) res.close();
		}
	}

}
