package com.vaguehope.morrigan.dlna;

import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.registry.DefaultRegistryListener;
import org.jupnp.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.extcd.ContentDirectoryHolder;
import com.vaguehope.morrigan.dlna.players.PlayerHolder;

public class DeviceWatcher extends DefaultRegistryListener {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceWatcher.class);

	private final PlayerHolder playerHolder;
	private final ContentDirectoryHolder contentDirectoryHolder;

	public DeviceWatcher (final PlayerHolder playerHolder, final ContentDirectoryHolder contentDirectoryHolder) {
		this.playerHolder = playerHolder;
		this.contentDirectoryHolder = contentDirectoryHolder;
	}

	@Override
	public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception e) {
		LOG.warn("remoteDeviceDiscoveryFailed: {}", device);
		super.remoteDeviceDiscoveryFailed(registry, device, e);
	}

	@Override
	public void remoteDeviceAdded (final Registry registry, final RemoteDevice device) {
		final RemoteService avTransport = UpnpHelper.findFirstServiceOfType(device, UpnpHelper.SERVICE_AVTRANSPORT);
		if (avTransport != null) {
			LOG.info("found: {} on {} (udn={}, maxAge={}s)",
					avTransport.getServiceId().getId(),
					device.getDetails().getFriendlyName(),
					device.getIdentity().getUdn(),
					device.getIdentity().getMaxAgeSeconds());
			this.playerHolder.addAvTransport(device, avTransport);
		}

		final RemoteService contentDirectory = UpnpHelper.findFirstServiceOfType(device, UpnpHelper.SERVICE_CONTENTDIRECTORY);
		if (contentDirectory != null) {
			LOG.info("found: {} on {} (udn={}, maxAge={}s)",
					contentDirectory.getServiceId().getId(),
					device.getDetails().getFriendlyName(),
					device.getIdentity().getUdn(),
					device.getIdentity().getMaxAgeSeconds());
			this.contentDirectoryHolder.addContentDirectory(device, contentDirectory);
		}

		super.remoteDeviceAdded(registry, device);
	}

	@Override
	public void remoteDeviceRemoved (final Registry registry, final RemoteDevice device) {
		LOG.info("lost: {} (udn={})",
				device.getDetails().getFriendlyName(),
				device.getIdentity().getUdn());
		this.playerHolder.removeAvTransport(device);
		this.contentDirectoryHolder.removeContentDirectory(device);

		super.remoteDeviceRemoved(registry, device);
	}

}
