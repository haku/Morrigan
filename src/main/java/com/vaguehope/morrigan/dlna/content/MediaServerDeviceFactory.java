package com.vaguehope.morrigan.dlna.content;

import java.io.IOException;

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
import org.jupnp.support.connectionmanager.ConnectionManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.SystemId;
import com.vaguehope.morrigan.dlna.UpnpHelper;
import com.vaguehope.morrigan.dlna.httpserver.MediaServer;
import com.vaguehope.morrigan.model.media.MediaFactory;

/**
 * Based on a class from WireMe and used under Apache 2 License.
 * See https://code.google.com/p/wireme/ for more details.
 */
public class MediaServerDeviceFactory {

	private static final String DEVICE_TYPE = "MediaServer";
	private static final int VERSION = 1;
	private static final String IDENTIFIER_STEM = "Morrigan-MediaServer";

	/**
	 * Shorter version of org.teleal.cling.model.Constants.MIN_ADVERTISEMENT_AGE_SECONDS.
	 * Remove when Cling 2.0 has a stable release.
	 * http://4thline.org/projects/mailinglists.html#nabble-td2183974
	 * http://4thline.org/projects/mailinglists.html#nabble-td2183974
	 * https://github.com/4thline/cling/issues/41
	 */
	private static final int MIN_ADVERTISEMENT_AGE_SECONDS = 300;

	private static final Logger LOG = LoggerFactory.getLogger(MediaServerDeviceFactory.class);

	private final LocalDevice localDevice;

	public MediaServerDeviceFactory (final SystemId systemId, final MediaFactory mediaFactory, final MediaServer mediaServer, final MediaFileLocator mediaFileLocator) throws ValidationException, IOException {
		final UDN usi = systemId.getUsi(IDENTIFIER_STEM);
		LOG.info("uniqueSystemIdentifier for {}: {}", DEVICE_TYPE, usi);

		final DeviceType type = new UDADeviceType(DEVICE_TYPE, VERSION);
		final DeviceDetails details = UpnpHelper.deviceDetails();
		final Icon icon = UpnpHelper.createDeviceIcon();

		final LocalService<ContentDirectoryService> contDirSrv = new AnnotationLocalServiceBinder().read(ContentDirectoryService.class);
		contDirSrv.setManager(new DefaultServiceManager<ContentDirectoryService>(contDirSrv, ContentDirectoryService.class) {
			@Override
			protected ContentDirectoryService createServiceInstance () {
				final ContentAdaptor contentAdaptor = new ContentAdaptor(mediaFactory, mediaServer, mediaFileLocator);
				return new ContentDirectoryService(contentAdaptor, new SearchEngine(contentAdaptor, mediaFactory));
			}

			@Override
			public void execute (final Command<ContentDirectoryService> cmd) throws Exception {
				try {
					super.execute(cmd);
				}
				catch (final Exception e) {
					LOG.warn("Action failed: " + cmd, e);
					throw e;
				}
			}
		});

		final LocalService<ConnectionManagerService> connManSrv = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
		connManSrv.setManager(new DefaultServiceManager<ConnectionManagerService>(connManSrv, ConnectionManagerService.class) {
			@Override
			public void execute (final Command<ConnectionManagerService> cmd) throws Exception {
				try {
					super.execute(cmd);
				}
				catch (final Exception e) {
					LOG.warn("Action failed: " + cmd, e);
					throw e;
				}
			}
		});

		this.localDevice = new LocalDevice(new DeviceIdentity(usi, MIN_ADVERTISEMENT_AGE_SECONDS), type, details, icon, new LocalService[] { contDirSrv, connManSrv });
	}

	public LocalDevice getDevice () {
		return this.localDevice;
	}

}
