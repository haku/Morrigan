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
	MediaDb createLocalMixedMediaDb (String name) throws MorriganException;

	/**
	 * Main instance.
	 */
	MediaDb getLocalMixedMediaDb (String fullFilePath) throws DbException;

	/**
	 * With a filter set: a view.
	 */
	MediaDb getLocalMixedMediaDb (String fullFilePath, String searchTerm) throws DbException;

	/**
	 * Same as getLocalMixedMediaDb().
	 */
	MediaDb getLocalMixedMediaDbBySerial (String serial) throws DbException;

	/**
	 * MID is like:
	 * LOCALMMDB/test.local.db3
	 */
	MediaList getMediaListByMid(String mid, String filter) throws DbException, MorriganException;

	MediaList getMediaListByRef(MediaListReference ref) throws DbException, MorriganException;
	MediaList getMediaListByRef(MediaListReference ref, String filter) throws DbException, MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Create an new instance with transactional behaviour.
	 */
	MediaDb getLocalMixedMediaDbTransactional (MediaDb lmmdb) throws DbException;

	MediaDb getMediaItemDbTransactional (MediaDb db) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	Collection<MediaListReference> getAllRemoteMixedMediaDbs ();
	RemoteMediaDb createRemoteMixedMediaDb (String mmdbUrl);
	RemoteMediaDb getRemoteMixedMediaDb (String dbName);
	RemoteMediaDb getRemoteMixedMediaDb (String dbName, URL url);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	MediaStorageLayer getStorageLayerWithNewItemFactory(String filepath) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	Collection<MediaListReference> getExternalLists();
	MediaList getExternalList(String id, String filter) throws MorriganException;
	MediaList getExternalListBySerial(String serial) throws MorriganException;
	void addExternalList(MediaList db);
	MediaList removeExternalList(String id);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	DurationData getNewDurationData (long duration, boolean complete);

	/*
	 * TODO merge these next two methods?
	 */
	MorriganTask getLocalMixedMediaDbUpdateTask (MediaDb library);
	MorriganTask getRemoteMixedMediaDbUpdateTask (RemoteMediaDb library);
	MorriganTask getMediaFileCopyTask (MediaList mediaItemList, List<MediaItem> mediaSelection, File targetDirectory);
	MorriganTask getNewCopyToLocalMmdbTask (MediaList fromList, Collection<MediaItem> itemsToCopy, MediaDb toDb);
	MorriganTask getSyncMetadataRemoteToLocalTask (MediaDb local, RemoteMediaDb remote);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void readTrackTags (MediaDb itemDb, MediaItem mlt, File file) throws IOException, MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
