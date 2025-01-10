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
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaStorageLayer;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.ErrorHelper;

public class ContentDirectoryHolder {

	private static final Logger LOG = LoggerFactory.getLogger(ContentDirectoryHolder.class);

	private final UpnpService upnpService;
	private final MediaFactory mediaFactory;
	private final Config config;

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final Map<ListRef, RemoteService> contentDirectories = new ConcurrentHashMap<>();

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
			for (final ListRef ref : this.contentDirectories.keySet()) {
				dispose(this.mediaFactory.removeExternalList(ref));
			}
			this.contentDirectories.clear();
		}
	}

	public void addContentDirectory (final RemoteDevice device, final RemoteService remoteService) {
		checkAlive();
		final ListRef ref = refForDevice(device);
		this.contentDirectories.put(ref, remoteService);
		try {
			final MediaStorageLayer storageLayer = this.mediaFactory.getStorageLayerWithNewItemFactory(getMetadataDbPath(ref).getAbsolutePath());
			final MetadataStorage storage = new MetadataStorage(storageLayer);
			final ContentDirectory cd = new ContentDirectory(this.upnpService.getControlPoint(), remoteService, storage);
			this.mediaFactory.addExternalList(new ContentDirectoryDb(ref, "", this.upnpService.getControlPoint(), device, remoteService, storage, cd));
		}
		catch (final DbException e) {
			LOG.warn("Failed to create storage: {}", ErrorHelper.oneLineCauseTrace(e));
		}
	}

	public void removeContentDirectory (final RemoteDevice device) {
		checkAlive();
		final ListRef ref = refForDevice(device);
		this.contentDirectories.remove(ref);
		dispose(this.mediaFactory.removeExternalList(ref));
	}

	private static ListRef refForDevice(final RemoteDevice device) {
		final String id = device.getIdentity().getUdn().getIdentifierString();
		return ListRef.forDlnaNode(id, ContentDirectory.ROOT_CONTENT_ID);
	}

	private static void dispose (final MediaList db) {
		if (db == null) return;
		db.dispose();
	}

	private static final String METADATA_DB_DIR = "dlnametadata";  // TODO move to Config class.

	private File getMetadataDbPath (final ListRef ref) {
		final File d = new File(this.config.getConfigDir(), METADATA_DB_DIR);
		if (!d.exists() && !d.mkdirs() && !d.exists()) throw new IllegalStateException("Failed to create direactory '" + d.getAbsolutePath() + "'.");
		return new File(d, ref.getListId());
	}

}
