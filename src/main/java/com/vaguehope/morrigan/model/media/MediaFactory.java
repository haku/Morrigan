package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.sqlitewrapper.DbException;


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
	IMediaItemDb createLocalMixedMediaDb (String name) throws MorriganException;

	/**
	 * Main instance.
	 */
	IMediaItemDb getLocalMixedMediaDb (String fullFilePath) throws DbException;

	/**
	 * With a filter set: a view.
	 */
	IMediaItemDb getLocalMixedMediaDb (String fullFilePath, String searchTerm) throws DbException;

	/**
	 * Same as getLocalMixedMediaDb().
	 */
	IMediaItemDb getLocalMixedMediaDbBySerial (String serial) throws DbException;

	/**
	 * MID is like:
	 * LOCALMMDB/test.local.db3
	 */
	IMediaItemList getMediaListByMid(String mid, String filter) throws DbException, MorriganException;

	IMediaItemList getMediaListByRef(MediaListReference ref) throws DbException, MorriganException;
	IMediaItemList getMediaListByRef(MediaListReference ref, String filter) throws DbException, MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Create an new instance with transactional behaviour.
	 */
	IMediaItemDb getLocalMixedMediaDbTransactional (IMediaItemDb lmmdb) throws DbException;

	IMediaItemDb getMediaItemDbTransactional (IMediaItemDb db) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	Collection<MediaListReference> getAllRemoteMixedMediaDbs ();
	IRemoteMixedMediaDb createRemoteMixedMediaDb (String mmdbUrl);
	IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName);
	IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName, URL url);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	IMediaItemStorageLayer getStorageLayer (String filepath) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	Collection<MediaListReference> getExternalLists();
	IMediaItemList getExternalList(String id, String filter) throws MorriganException;
	IMediaItemList getExternalListBySerial(String serial) throws MorriganException;
	void addExternalList(IMediaItemList db);
	IMediaItemList removeExternalList(String id);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	DurationData getNewDurationData (long duration, boolean complete);

	/*
	 * TODO merge these next two methods?
	 */
	MorriganTask getLocalMixedMediaDbUpdateTask (IMediaItemDb library);
	MorriganTask getRemoteMixedMediaDbUpdateTask (IRemoteMixedMediaDb library);
	MorriganTask getMediaFileCopyTask (IMediaItemList mediaItemList, List<IMediaItem> mediaSelection, File targetDirectory);
	MorriganTask getNewCopyToLocalMmdbTask (IMediaItemList fromList, Collection<IMediaItem> itemsToCopy, IMediaItemDb toDb);
	MorriganTask getSyncMetadataRemoteToLocalTask (IMediaItemDb local, IRemoteMixedMediaDb remote);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void readTrackTags (IMediaItemDb itemDb, IMediaItem mlt, File file) throws IOException, MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
