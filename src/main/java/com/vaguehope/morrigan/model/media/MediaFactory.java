package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ListRef.ListType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.MorriganTask;


/**
 * TODO remove remains of play-lists - they don't even work anyway.
 */
public interface MediaFactory {

	Collection<ListRefWithTitle> allLists();
	Collection<ListRefWithTitle> allListsOfType(ListType type);
	MediaList getList(ListRef listRef) throws MorriganException;

	MediaDb createLocalMixedMediaDb (String name) throws MorriganException;
	MediaDb getLocalMixedMediaDb (String fullFilePath) throws DbException;
	MediaDb getLocalMixedMediaDb (String fullFilePath, String searchTerm) throws DbException;
	MediaDb getLocalMixedMediaDbTransactional (MediaDb lmmdb) throws DbException;
	MediaDb getMediaItemDbTransactional (MediaDb db) throws DbException;

	MediaStorageLayer getStorageLayerWithNewItemFactory(String filepath) throws DbException;

	void addExternalList(MediaList db);
	MediaList removeExternalList(ListRef listRef);

	@Deprecated
	DurationData getNewDurationData (long duration, boolean complete);

	/*
	 * TODO merge these next two methods?
	 */
	MorriganTask getLocalMixedMediaDbUpdateTask (MediaDb library);
	MorriganTask getRemoteMixedMediaDbUpdateTask (RemoteMediaDb library);
	MorriganTask getMediaFileCopyTask (MediaList mediaItemList, List<MediaItem> mediaSelection, File targetDirectory);
	MorriganTask getNewCopyToLocalMmdbTask (MediaList fromList, Collection<MediaItem> itemsToCopy, MediaDb toDb);
	MorriganTask getSyncMetadataRemoteToLocalTask (MediaDb local, RemoteMediaDb remote);

	void readTrackTags (MediaDb itemDb, MediaItem mlt, File file) throws IOException, MorriganException;

}
