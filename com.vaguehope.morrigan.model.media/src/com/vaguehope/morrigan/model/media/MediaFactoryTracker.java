package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.sqlitewrapper.DbException;

public class MediaFactoryTracker implements MediaFactory {

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final ServiceTracker<MediaFactory, MediaFactory> playerReaderTracker;

	public MediaFactoryTracker (final BundleContext context) {
		this.playerReaderTracker = new ServiceTracker<MediaFactory, MediaFactory>(context, MediaFactory.class, null);
		this.playerReaderTracker.open();
	}

	public void dispose () {
		this.alive.set(false);
		this.playerReaderTracker.close();
	}

	private void checkAlive () {
		if (!this.alive.get()) throw new IllegalStateException(this.getClass().getName() + " is disposed.");
	}

	private MediaFactory getServiceOptional () {
		checkAlive();
		return this.playerReaderTracker.getService();
	}

	private MediaFactory getService () {
		MediaFactory service = getServiceOptional();
		if (service == null) throw new IllegalStateException("MediaFactory service not available.");
		return service;
	}

	@Override
	public Collection<MediaListReference> getAllLocalMixedMediaDbs () {
		MediaFactory service = getServiceOptional();
		return (service == null) ? Collections.<MediaListReference>emptyList() : service.getAllLocalMixedMediaDbs();
	}

	@Override
	public ILocalMixedMediaDb createLocalMixedMediaDb (final String name) throws MorriganException {
		return getService().createLocalMixedMediaDb(name);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDb (final String fullFilePath) throws DbException {
		return getService().getLocalMixedMediaDb(fullFilePath);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDb (final String fullFilePath, final String searchTerm) throws DbException {
		return getService().getLocalMixedMediaDb(fullFilePath, searchTerm);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDbBySerial (final String serial) throws DbException {
		return getService().getLocalMixedMediaDbBySerial(serial);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDbTransactional (final ILocalMixedMediaDb lmmdb) throws DbException {
		return getService().getLocalMixedMediaDbTransactional(lmmdb);
	}

	@Override
	public IMediaItemDb<?, ?> getMediaItemDbTransactional (final IMediaItemDb<?, ?> db) throws DbException {
		return getService().getMediaItemDbTransactional(db);
	}

	@Override
	public Collection<MediaListReference> getAllRemoteMixedMediaDbs () {
		MediaFactory service = getServiceOptional();
		return (service == null) ? Collections.<MediaListReference>emptyList() : service.getAllRemoteMixedMediaDbs();
	}

	@Override
	public IRemoteMixedMediaDb createRemoteMixedMediaDb (final String mmdbUrl) {
		return getService().createRemoteMixedMediaDb(mmdbUrl);
	}

	@Override
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (final String dbName) {
		return getService().getRemoteMixedMediaDb(dbName);
	}

	@Override
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (final String dbName, final URL url) {
		return getService().getRemoteMixedMediaDb(dbName, url);
	}

	@Override
	public Collection<MediaListReference> getExternalDbs () {
		return getService().getExternalDbs();
	}

	@Override
	public IMixedMediaDb getExternalDb (final String id) {
		return getService().getExternalDb(id);
	}

	@Override
	public void addExternalDb (final IMixedMediaDb db) {
		getService().addExternalDb(db);
	}

	@Override
	public void removeExternalDb (final String id) {
		getService().removeExternalDb(id);
	}

	@Override
	public DurationData getNewDurationData (final long duration, final boolean complete) {
		return getService().getNewDurationData(duration, complete);
	}

	@Override
	public MorriganTask getLocalMixedMediaDbUpdateTask (final ILocalMixedMediaDb library) {
		return getService().getLocalMixedMediaDbUpdateTask(library);
	}

	@Override
	public MorriganTask getRemoteMixedMediaDbUpdateTask (final IRemoteMixedMediaDb library) {
		return getService().getRemoteMixedMediaDbUpdateTask(library);
	}

	@Override
	public <T extends IMediaItem> MorriganTask getMediaFileCopyTask (final IMediaItemList<T> mediaItemList, final List<T> mediaSelection, final File targetDirectory) {
		return getService().getMediaFileCopyTask(mediaItemList, mediaSelection, targetDirectory);
	}

	@Override
	public <T extends IMediaItem> MorriganTask getNewCopyToLocalMmdbTask (final IMediaItemList<T> fromList, final Collection<T> itemsToCopy, final ILocalMixedMediaDb toDb) {
		return getService().getNewCopyToLocalMmdbTask(fromList, itemsToCopy, toDb);
	}

	@Override
	public MorriganTask getSyncMetadataRemoteToLocalTask (final ILocalMixedMediaDb local, final IRemoteMixedMediaDb remote) {
		return getService().getSyncMetadataRemoteToLocalTask(local, remote);
	}

	@Override
	public void readTrackTags (final IMediaItemDb<?, ?> itemDb, final IMediaTrack mlt, final File file) throws IOException, MorriganException {
		getService().readTrackTags(itemDb, mlt, file);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

}
