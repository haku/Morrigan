package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.tasks.IMorriganTask;
import com.vaguehope.sqlitewrapper.DbException;


/**
 * TODO remove remains of play-lists - they don't even work anyway.
 */
public interface MediaFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * List them all.
	 */
	Collection<MediaListReference> getAllLocalMixedMediaDbs ();

	/**
	 * Create a new one.
	 */
	ILocalMixedMediaDb createLocalMixedMediaDb (String name) throws MorriganException;

	/**
	 * Main instance.
	 */
	ILocalMixedMediaDb getLocalMixedMediaDb (String fullFilePath) throws DbException;

	/**
	 * With a filter set: a view.
	 */
	ILocalMixedMediaDb getLocalMixedMediaDb (String fullFilePath, String searchTerm) throws DbException;

	/**
	 * Same as getLocalMixedMediaDb().
	 */
	ILocalMixedMediaDb getLocalMixedMediaDbBySerial (String serial) throws DbException;

	/**
	 * Create an new instance with transactional behaviour.
	 */
	ILocalMixedMediaDb getLocalMixedMediaDbTransactional (ILocalMixedMediaDb lmmdb) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	IMediaItemDb<?,?> getMediaItemDbTransactional (IMediaItemDb<?,?> db) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	Collection<MediaListReference> getAllRemoteMixedMediaDbs ();
	IRemoteMixedMediaDb createRemoteMixedMediaDb (String mmdbUrl);
	IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName);
	IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName, URL url);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	DurationData getNewDurationData (long duration, boolean complete);

	/*
	 * TODO merge these next two methods?
	 */
	IMorriganTask getLocalMixedMediaDbUpdateTask (ILocalMixedMediaDb library);
	IMorriganTask getRemoteMixedMediaDbUpdateTask (IRemoteMixedMediaDb library);
	<T extends IMediaItem> IMorriganTask getMediaFileCopyTask (IMediaItemList<T> mediaItemList, List<T> mediaSelection, File targetDirectory);
	<T extends IMediaItem> IMorriganTask getNewCopyToLocalMmdbTask (IMediaItemList<T> fromList, Collection<T> itemsToCopy, ILocalMixedMediaDb toDb);
	IMorriganTask getSyncMetadataRemoteToLocalTask (ILocalMixedMediaDb local, IRemoteMixedMediaDb remote);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void readTrackTags (IMediaItemDb<?,?> itemDb, IMediaTrack mlt, File file) throws IOException, MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
