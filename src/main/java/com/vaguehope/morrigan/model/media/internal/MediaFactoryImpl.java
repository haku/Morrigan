package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.ListRef.ListType;
import com.vaguehope.morrigan.model.media.ListRefWithTitle;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaStorageLayer;
import com.vaguehope.morrigan.model.media.internal.db.CopyToLocalMmdbTask;
import com.vaguehope.morrigan.model.media.internal.db.DefaultMediaItemFactory;
import com.vaguehope.morrigan.model.media.internal.db.LocalMediaDbFactory;
import com.vaguehope.morrigan.model.media.internal.db.LocalMediaDbHelper;
import com.vaguehope.morrigan.model.media.internal.db.LocalMediaDbUpdateTask;
import com.vaguehope.morrigan.model.media.internal.db.MediaSqliteLayerFactory;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.MorriganTask;

public class MediaFactoryImpl implements MediaFactory {

	private final Config config;
	private final LocalMediaDbUpdateTask.Factory localMixedMediaDbUpdateTaskFactory;
	private final Map<ListRef, MediaDb> addedLocals = new ConcurrentHashMap<>();
	private final Map<ListRef, MediaList> externalListsByListRef = new ConcurrentSkipListMap<>();

	public MediaFactoryImpl (final Config config, final PlaybackEngineFactory playbackEngineFactoryTracker) {
		this.config = config;
		this.localMixedMediaDbUpdateTaskFactory = new LocalMediaDbUpdateTask.Factory(playbackEngineFactoryTracker, this);
	}

	@Override
	public Collection<ListRefWithTitle> allLists() {
		final Collection<ListRefWithTitle> ret = new ArrayList<>();

		ret.addAll(LocalMediaDbHelper.getAllLocalDbs(this.config));
		for (final MediaDb db : this.addedLocals.values()) {
			ret.add(new ListRefWithTitle(db.getListRef(), db.getListName()));
		}

		for (final MediaList list : this.externalListsByListRef.values()) {
			ret.add(new ListRefWithTitle(list.getListRef(), list.getListName()));
		}

		return ret;
	}

	@Override
	public Collection<ListRefWithTitle> allListsOfType(final ListType type) {
		return allLists().stream().filter(l -> l.getListRef().getType() == type).collect(Collectors.toList());
	}

	@Override
	public MediaList getList(final ListRef listRef) throws MorriganException {
		switch (listRef.getType()) {
		case LOCAL:
			final MediaDb local = this.addedLocals.get(listRef);
			if (local != null) return local;

			final String localFile = LocalMediaDbHelper.getFullPathToLocalDb(this.config, listRef.getListId());
			if (StringUtils.isBlank(listRef.getSearch())) return getLocalMixedMediaDb(localFile);
			return LocalMediaDbFactory.getView(localFile, listRef.getSearch());

		case RPC:
		case DLNA:
			final MediaList list = this.externalListsByListRef.get(listRef);
			if (list != null) return list;

			final MediaList root = this.externalListsByListRef.get(listRef.toRoot());
			if (root != null) return root.resolveRef(listRef);

			return null;

		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * For testing use only.  Can use to add in mock implementations.
	 */
	public void addLocalMixedMediaDb (final MediaDb db) {
		if (db.getListRef().getType() != ListType.LOCAL) throw new IllegalArgumentException();
		this.addedLocals.put(db.getListRef(), db);
	}

	@Override
	public void addExternalList (final MediaList db) {
		this.externalListsByListRef.put(db.getListRef(), db);
	}

	@Override
	public MediaList removeExternalList (final ListRef listRef) {
		return this.externalListsByListRef.remove(listRef);
	}

	@Override
	public MediaDb createLocalMixedMediaDb (final String name) throws MorriganException {
		return LocalMediaDbHelper.createLocalDb(this.config, name);
	}

	@Override
	public MediaDb getLocalMixedMediaDb (final String fullFilePath) throws DbException {
		if (!new File(fullFilePath).isFile()) throw new DbException("File not found: " + fullFilePath);
		return LocalMediaDbFactory.getMain(fullFilePath);
	}

	@Override
	public MediaDb getLocalMixedMediaDb (final String fullFilePath, final String filter) throws DbException {
		if (!new File(fullFilePath).isFile()) throw new DbException("File not found: " + fullFilePath);
		if (filter == null || filter.length() < 1) return getLocalMixedMediaDb(fullFilePath);
		return LocalMediaDbFactory.getView(fullFilePath, filter);
	}

	@Override
	public MediaDb getLocalMixedMediaDbTransactional (final MediaDb lmmdb) throws DbException {
		return LocalMediaDbFactory.getTransactional(lmmdb.getDbPath());
	}


	@Override
	public MediaDb getMediaItemDbTransactional (final MediaDb db) throws DbException {
		if (db.getListRef().getType() == ListType.LOCAL) {
			return LocalMediaDbFactory.getTransactional(db.getDbPath());
		}
		throw new IllegalArgumentException("Can't create transactional connection to DB: " + db.getListRef());
	}

	@Override
	public MediaStorageLayer getStorageLayerWithNewItemFactory(final String filepath) throws DbException {
		final DefaultMediaItemFactory itemFactory = new DefaultMediaItemFactory(null);
		return MediaSqliteLayerFactory.getTransactional(filepath, itemFactory);
	}

	@Override
	public MorriganTask getLocalMixedMediaDbUpdateTask (final MediaDb library) {
		return this.localMixedMediaDbUpdateTaskFactory.manufacture(library);
	}

	@Override
	public MorriganTask getMediaFileCopyTask (final MediaList mediaItemList, final List<MediaItem> mediaSelection, final File targetDirectory) {
		return new MediaFileCopyTask(mediaItemList, mediaSelection, targetDirectory);
	}

	@Override
	public MorriganTask getNewCopyToLocalMmdbTask (final MediaList fromList, final Collection<MediaItem> itemsToCopy, final MediaDb toDb) {
		return new CopyToLocalMmdbTask(fromList, itemsToCopy, toDb, this.config);
	}

}
