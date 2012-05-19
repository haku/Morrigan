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
import com.vaguehope.morrigan.tasks.IMorriganTask;
import com.vaguehope.sqlitewrapper.DbException;

public class MediaFactoryTracker implements MediaFactory {

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final ServiceTracker<MediaFactory, MediaFactory> playerReaderTracker;

	public MediaFactoryTracker (BundleContext context) {
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
		MediaFactory service = this.playerReaderTracker.getService();
		return service;
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
	public ILocalMixedMediaDb createLocalMixedMediaDb (String name) throws MorriganException {
		return getService().createLocalMixedMediaDb(name);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDb (String fullFilePath) throws DbException {
		return getService().getLocalMixedMediaDb(fullFilePath);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDb (String fullFilePath, String searchTerm) throws DbException {
		return getService().getLocalMixedMediaDb(fullFilePath, searchTerm);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDbBySerial (String serial) throws DbException {
		return getService().getLocalMixedMediaDbBySerial(serial);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDbTransactional (ILocalMixedMediaDb lmmdb) throws DbException {
		return getService().getLocalMixedMediaDbTransactional(lmmdb);
	}

	@Override
	public IMediaItemDb<?, ?> getMediaItemDbTransactional (IMediaItemDb<?, ?> db) throws DbException {
		return getService().getMediaItemDbTransactional(db);
	}

	@Override
	public Collection<MediaListReference> getAllRemoteMixedMediaDbs () {
		MediaFactory service = getServiceOptional();
		return (service == null) ? Collections.<MediaListReference>emptyList() : service.getAllRemoteMixedMediaDbs();
	}

	@Override
	public IRemoteMixedMediaDb createRemoteMixedMediaDb (String mmdbUrl) {
		return getService().createRemoteMixedMediaDb(mmdbUrl);
	}

	@Override
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName) {
		return getService().getRemoteMixedMediaDb(dbName);
	}

	@Override
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName, URL url) {
		return getService().getRemoteMixedMediaDb(dbName, url);
	}

	@Override
	public DurationData getNewDurationData (long duration, boolean complete) {
		return getService().getNewDurationData(duration, complete);
	}

	@Override
	public IMorriganTask getLocalMixedMediaDbUpdateTask (ILocalMixedMediaDb library) {
		return getService().getLocalMixedMediaDbUpdateTask(library);
	}

	@Override
	public IMorriganTask getRemoteMixedMediaDbUpdateTask (IRemoteMixedMediaDb library) {
		return getService().getRemoteMixedMediaDbUpdateTask(library);
	}

	@Override
	public <T extends IMediaItem> IMorriganTask getMediaFileCopyTask (IMediaItemList<T> mediaItemList, List<T> mediaSelection, File targetDirectory) {
		return getService().getMediaFileCopyTask(mediaItemList, mediaSelection, targetDirectory);
	}

	@Override
	public <T extends IMediaItem> IMorriganTask getNewCopyToLocalMmdbTask (IMediaItemList<T> fromList, Collection<T> itemsToCopy, ILocalMixedMediaDb toDb) {
		return getService().getNewCopyToLocalMmdbTask(fromList, itemsToCopy, toDb);
	}

	@Override
	public IMorriganTask getSyncMetadataRemoteToLocalTask (ILocalMixedMediaDb local, IRemoteMixedMediaDb remote) {
		return getService().getSyncMetadataRemoteToLocalTask(local, remote);
	}

	@Override
	public void readTrackTags (IMediaItemDb<?, ?> itemDb, IMediaTrack mlt, File file) throws IOException, MorriganException {
		getService().readTrackTags(itemDb, mlt, file);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

}
