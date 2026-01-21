package morrigan.model.media;

import java.io.File;
import java.util.Collection;
import java.util.List;

import morrigan.model.exceptions.MorriganException;
import morrigan.model.media.ListRef.ListType;
import morrigan.sqlitewrapper.DbException;
import morrigan.tasks.MorriganTask;


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

	/*
	 * TODO merge these next two methods?
	 */
	MorriganTask getLocalMixedMediaDbUpdateTask (MediaDb library);
	MorriganTask getMediaFileCopyTask (MediaList mediaItemList, List<MediaItem> mediaSelection, File targetDirectory);
	MorriganTask getNewCopyToLocalMmdbTask (MediaList fromList, Collection<MediaItem> itemsToCopy, MediaDb toDb);

}
