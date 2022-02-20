package com.vaguehope.morrigan.dlna.extcd;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaStorageLayer;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.util.ErrorHelper;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class ContentDirectoryHolder {

	private static final Logger LOG = LoggerFactory.getLogger(ContentDirectoryHolder.class);

	private final ControlPoint controlPoint;
	private final MediaFactory mediaFactory;
	private final Config config;

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final Map<String, RemoteService> contentDirectories = new ConcurrentHashMap<>();

	public ContentDirectoryHolder (final ControlPoint controlPoint, final MediaFactory mediaFactory, final Config config) {
		this.controlPoint = controlPoint;
		this.mediaFactory = mediaFactory;
		this.config = config;
	}

	private void checkAlive () {
		if (!this.alive.get()) throw new IllegalStateException();
	}

	public void dispose () {
		if (this.alive.compareAndSet(true, false)) {
			for (final String id : this.contentDirectories.keySet()) {
				dispose(this.mediaFactory.removeExternalDb(id));
			}
			this.contentDirectories.clear();
		}
	}

	public void addContentDirectory (final RemoteDevice device, final RemoteService contentDirectory) {
		checkAlive();
		final String id = idForDevice(device);
		this.contentDirectories.put(id, contentDirectory);
		try {
			final IMixedMediaStorageLayer storage = this.mediaFactory.getStorageLayer(getMetadataDbPath(id).getAbsolutePath());
			this.mediaFactory.addExternalDb(new ContentDirectoryDb(id, this.controlPoint, device, contentDirectory, storage));
		}
		catch (final DbException e) {
			LOG.warn("Failed to create storage: {}", ErrorHelper.oneLineCauseTrace(e));
		}
	}

	public void removeContentDirectory (final RemoteDevice device) {
		checkAlive();
		final String id = idForDevice(device);
		this.contentDirectories.remove(id);
		dispose(this.mediaFactory.removeExternalDb(id));
	}

	private static String idForDevice (final RemoteDevice device) {
		return device.getIdentity().getUdn().getIdentifierString();
	}

	private static void dispose (final IMixedMediaDb db) {
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
