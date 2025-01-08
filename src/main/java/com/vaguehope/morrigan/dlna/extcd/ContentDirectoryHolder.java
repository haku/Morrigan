package com.vaguehope.morrigan.dlna.extcd;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jupnp.UpnpService;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaStorageLayer;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.ErrorHelper;

public class ContentDirectoryHolder {

	private static final Logger LOG = LoggerFactory.getLogger(ContentDirectoryHolder.class);

	private final UpnpService upnpService;
	private final MediaFactory mediaFactory;
	private final Config config;

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final Map<String, RemoteService> contentDirectories = new ConcurrentHashMap<>();

	public ContentDirectoryHolder (final UpnpService upnpService, final MediaFactory mediaFactory, final Config config) {
		this.upnpService = upnpService;
		this.mediaFactory = mediaFactory;
		this.config = config;
	}

	private void checkAlive () {
		if (!this.alive.get()) throw new IllegalStateException();
	}

	public void dispose () {
		if (this.alive.compareAndSet(true, false)) {
			for (final String id : this.contentDirectories.keySet()) {
				dispose(this.mediaFactory.removeExternalList(id));
			}
			this.contentDirectories.clear();
		}
	}

	public void addContentDirectory (final RemoteDevice device, final RemoteService remoteService) {
		checkAlive();
		final String id = idForDevice(device);
		this.contentDirectories.put(id, remoteService);
		try {
			final MediaStorageLayer storageLayer = this.mediaFactory.getStorageLayerWithNewItemFactory(getMetadataDbPath(id).getAbsolutePath());
			final MetadataStorage storage = new MetadataStorage(storageLayer);
			final ContentDirectory cd = new ContentDirectory(this.upnpService.getControlPoint(), remoteService, storage);
			this.mediaFactory.addExternalList(new ContentDirectoryDb(id, ContentDirectory.ROOT_CONTENT_ID, "",
					this.upnpService.getControlPoint(), device, remoteService, storage, cd));
		}
		catch (final DbException e) {
			LOG.warn("Failed to create storage: {}", ErrorHelper.oneLineCauseTrace(e));
		}
	}

	public void removeContentDirectory (final RemoteDevice device) {
		checkAlive();
		final String id = idForDevice(device);
		this.contentDirectories.remove(id);
		dispose(this.mediaFactory.removeExternalList(id));
	}

	private static String idForDevice (final RemoteDevice device) {
		return device.getIdentity().getUdn().getIdentifierString();
	}

	private static void dispose (final MediaList db) {
		if (db == null) return;
		db.dispose();
	}

	private static final String METADATA_DB_DIR = "dlnametadata";  // TODO move to Config class.

	private File getMetadataDbPath (final String id) {
		final File d = new File(this.config.getConfigDir(), METADATA_DB_DIR);
		if (!d.exists() && !d.mkdirs() && !d.exists()) throw new IllegalStateException("Failed to create direactory '" + d.getAbsolutePath() + "'.");
		return new File(d, id);
	}

}
