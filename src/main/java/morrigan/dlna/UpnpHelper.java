package morrigan.dlna;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;

public final class UpnpHelper {

	public static final String SERVICE_AVTRANSPORT = "AVTransport";
	public static final String SERVICE_CONTENTDIRECTORY = "ContentDirectory";
	public static final String SERVICE_RENDERINGCONTROL = "RenderingControl";

	private static final String METADATA_MANUFACTURER = "VagueHope";
	public static final String METADATA_MODEL_NAME = "Morrigan";
	private static final String METADATA_MODEL_DESCRIPTION = "Morrigan MediaServer";
	private static final String METADATA_MODEL_NUMBER = "v1";

	private UpnpHelper () {}

	public static String idFromRemoteService (final RemoteService rs) {
		return String.format("%s-%s",
				rs.getDevice().getIdentity().getUdn().getIdentifierString(),
				rs.getServiceId().getId())
				.replaceAll("[^a-zA-Z0-9-]", "_");
	}

	public static String remoteServiceUid (final RemoteService rs) {
		return String.format("%s/%s", rs.getDevice().getIdentity().getUdn(), rs.getServiceId().getId());
	}

	public static ThreadLocal<SimpleDateFormat> DC_DATE_FORMAT = new ThreadLocal<>() {
		@Override
		protected SimpleDateFormat initialValue () {
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			return dateFormat;
		}
	};

	public static RemoteService findFirstServiceOfType (final RemoteDevice rd, final String typeToFind) {
		for (final RemoteService rs : rd.getServices()) {
			if (typeToFind.equals(rs.getServiceType().getType())) return rs;
		}
		return null;
	}

	public static DeviceDetails deviceDetails() throws UnknownHostException {
		return deviceDetails(null);
	}

	public static DeviceDetails deviceDetails(final String nameSuffix) throws UnknownHostException {
		String name = METADATA_MODEL_NAME;
		if (nameSuffix != null) name += " " + nameSuffix;

		return new DeviceDetails(
				name + " (" + InetAddress.getLocalHost().getHostName() + ")",
				new ManufacturerDetails(METADATA_MANUFACTURER),
				new ModelDetails(METADATA_MODEL_NAME, METADATA_MODEL_DESCRIPTION, METADATA_MODEL_NUMBER));
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
