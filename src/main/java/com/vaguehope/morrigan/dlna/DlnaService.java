package com.vaguehope.morrigan.dlna;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.resource.IconResource;
import org.fourthline.cling.model.resource.Resource;
import org.fourthline.cling.protocol.ProtocolFactory;
import org.fourthline.cling.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.content.MediaFileLocator;
import com.vaguehope.morrigan.dlna.content.MediaServerDeviceFactory;
import com.vaguehope.morrigan.dlna.extcd.ContentDirectoryHolder;
import com.vaguehope.morrigan.dlna.httpserver.MediaServer;
import com.vaguehope.morrigan.dlna.players.PlayerHolder;
import com.vaguehope.morrigan.dlna.util.NetHelper;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.PlayerStateStorage;
import com.vaguehope.morrigan.server.ServerConfig;
import com.vaguehope.morrigan.util.DaemonThreadFactory;
import com.vaguehope.morrigan.util.StringHelper;

public class DlnaService {

	private static final int BG_THREADS = 3;
	private static final Logger LOG = LoggerFactory.getLogger(DlnaService.class);

	private final ServerConfig serverConfig;
	private final MediaFactory mediaFactory;
	private final PlayerRegister playerRegister;

	private MediaServer mediaServer;
	private PlayerHolder playerHolder;
	private UpnpService upnpService;
	private ContentDirectoryHolder contentDirectoryHolder;
	private ScheduledExecutorService scheduledExecutor;

	public DlnaService(final ServerConfig serverConfig, final MediaFactory mediaFactory, final PlayerRegister playerRegister) {
		this.serverConfig = serverConfig;
		this.mediaFactory = mediaFactory;
		this.playerRegister = playerRegister;
	}

	public void start() throws IOException, ValidationException {
		final InetAddress bindAddress = findBindAddress();
		if (bindAddress == null) throw new IllegalStateException("Failed to find bind address.");

		this.scheduledExecutor = Executors.newScheduledThreadPool(BG_THREADS, new DaemonThreadFactory("dlna"));

		final MediaFileLocator mediaFileLocator = new MediaFileLocator(this.mediaFactory);

		this.mediaServer = new MediaServer(mediaFileLocator, bindAddress);
		this.mediaServer.start();

		this.upnpService = makeUpnpServer();
		this.playerHolder = new PlayerHolder(
				this.playerRegister,
				this.upnpService.getControlPoint(),
				this.mediaServer,
				mediaFileLocator,
				new PlayerStateStorage(this.mediaFactory, this.scheduledExecutor),
				this.scheduledExecutor);

		this.upnpService.getRegistry().addDevice(new MediaServerDeviceFactory(
				InetAddress.getLocalHost().getHostName(),
				this.mediaFactory,
				this.mediaServer,
				mediaFileLocator
				).getDevice());

		this.contentDirectoryHolder = new ContentDirectoryHolder(this.upnpService.getControlPoint(), this.mediaFactory);

		this.upnpService.getRegistry().addListener(new DeviceWatcher(this.playerHolder, this.contentDirectoryHolder));
		this.upnpService.getControlPoint().search();

		LOG.info("DLNA started.");
	}

	private InetAddress findBindAddress () throws IOException {
		final InetAddress address;
		final String cfgBindIp = this.serverConfig.getBindIp();
		if (StringHelper.notBlank(cfgBindIp)) {
			address = InetAddress.getByName(cfgBindIp);
			LOG.info("using address: {}", address);
		}
		else {
			final List<InetAddress> addresses = NetHelper.getIpAddresses();
			address = addresses.iterator().next();
			LOG.info("addresses: {} using address: {}", addresses, address);
		}
		return address;
	}

	private static UpnpService makeUpnpServer () throws IOException {
		final Map<String, Resource<?>> pathToRes = new HashMap<>();

		final Icon icon = MediaServerDeviceFactory.createDeviceIcon();
		final IconResource iconResource = new IconResource(icon.getUri(), icon);
		pathToRes.put("/icon.png", iconResource);

		return new UpnpServiceImpl() {
			@Override
			protected Registry createRegistry (final ProtocolFactory pf) {
				return new RegistryImplWithOverrides(this, pathToRes);
			}
		};
	}

}
