package com.vaguehope.morrigan.dlna.content;

import java.io.IOException;
import java.io.InputStream;

import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.Command;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	static final String METADATA_MODEL_NAME = "Morrigan";
	private static final String METADATA_MANUFACTURER = "VagueHope";
	private static final String METADATA_MODEL_DESCRIPTION = "Morrigan MediaServer";
	private static final String METADATA_MODEL_NUMBER = "v1";

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

	public MediaServerDeviceFactory (final String hostName, final MediaFactory mediaFactory, final MediaServer mediaServer, final MediaFileLocator mediaFileLocator) throws ValidationException, IOException {
		final UDN usi = UDN.uniqueSystemIdentifier(IDENTIFIER_STEM);
		LOG.info("uniqueSystemIdentifier: {}", usi);
		final DeviceType type = new UDADeviceType(DEVICE_TYPE, VERSION);
		final DeviceDetails details = new DeviceDetails(METADATA_MODEL_NAME + " (" + hostName + ")",
				new ManufacturerDetails(METADATA_MANUFACTURER),
				new ModelDetails(METADATA_MODEL_NAME, METADATA_MODEL_DESCRIPTION, METADATA_MODEL_NUMBER));
		final Icon icon = createDeviceIcon();

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

	public static Icon createDeviceIcon () throws IOException {
		final InputStream res = MediaServerDeviceFactory.class.getResourceAsStream("/icon.png");
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
