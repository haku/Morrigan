package com.vaguehope.morrigan.dlna.players;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.Command;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;
import org.jupnp.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.jupnp.support.connectionmanager.ConnectionManagerService;
import org.jupnp.support.lastchange.LastChange;
import org.jupnp.support.lastchange.LastChangeAwareServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.SystemId;
import com.vaguehope.morrigan.dlna.UpnpHelper;
import com.vaguehope.morrigan.player.Player;


/**
 * expose players as DLNA players so they can be remote controlled, eg via home assistant.
 */
public class PlayerControlBridgeFactory {

	private static final String DEVICE_TYPE = "MediaRenderer";
	private static final int VERSION = 1;
	private static final String IDENTIFIER_STEM = "Morrigan-MediaRenderer";

	/**
	 * Shorter version of org.teleal.cling.model.Constants.MIN_ADVERTISEMENT_AGE_SECONDS.
	 * Remove when Cling 2.0 has a stable release.
	 * http://4thline.org/projects/mailinglists.html#nabble-td2183974
	 * http://4thline.org/projects/mailinglists.html#nabble-td2183974
	 * https://github.com/4thline/cling/issues/41
	 */
	private static final int MIN_ADVERTISEMENT_AGE_SECONDS = 300;

	private static final Logger LOG = LoggerFactory.getLogger(PlayerControlBridgeFactory.class);

	public static LocalDevice makeMediaRendererDevice (final SystemId systemId, final Player player, final DlnaPlayingParamsFactory dlnaPlayingParamsFactory, final ScheduledExecutorService schEs) throws IOException, ValidationException {
		final UDN usi = systemId.getUsi(IDENTIFIER_STEM + player.getId());
		LOG.info("uniqueSystemIdentifier for player {}: {}", player.getId(), usi);

		final DeviceType type = new UDADeviceType(DEVICE_TYPE, VERSION);
		final DeviceDetails details = UpnpHelper.deviceDetails(player.getName() + " Player");
		final Icon icon = UpnpHelper.createDeviceIcon();

		final AnnotationLocalServiceBinder binder = new AnnotationLocalServiceBinder();

		final LocalService<ConnectionManagerService> connManSrv = binder.read(ConnectionManagerService.class);
		connManSrv.setManager(new DefaultServiceManager<>(connManSrv, ConnectionManagerService.class));

		final LocalService<PlayerControlBridgeAVTransportService> avtSrv = binder.read(PlayerControlBridgeAVTransportService.class);
		final PlayerControlBridgeAVTransportService avTransportService = new PlayerControlBridgeAVTransportService(
				new LastChange(new AVTransportLastChangeParser()),
				player,
				dlnaPlayingParamsFactory);
		avtSrv.setManager(new LastChangeAwareServiceManager<PlayerControlBridgeAVTransportService>(avtSrv, new AVTransportLastChangeParser()) {
			@Override
			protected PlayerControlBridgeAVTransportService createServiceInstance () throws Exception {
				return avTransportService;
			}

			@Override
			public void execute (final Command<PlayerControlBridgeAVTransportService> cmd) throws Exception {
				try {
					super.execute(cmd);
				}
				catch (final Exception e) {
					LOG.warn("Action failed: " + cmd, e);
					throw e;
				}
			}
		});

		final Runnable fireLastChange = () -> ((LastChangeAwareServiceManager<?>) avtSrv.getManager()).fireLastChange();
		schEs.scheduleWithFixedDelay(fireLastChange, 1, 1, TimeUnit.SECONDS);

		final LocalDevice device = new LocalDevice(new DeviceIdentity(usi, MIN_ADVERTISEMENT_AGE_SECONDS), type, details, icon, new LocalService[] { avtSrv, connManSrv });
		return device;
	}

}
