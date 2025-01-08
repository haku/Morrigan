package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaStorageLayer;
import com.vaguehope.morrigan.model.media.RemoteMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.internal.db.CopyToLocalMmdbTask;
import com.vaguehope.morrigan.model.media.internal.db.DefaultMediaItemFactory;
import com.vaguehope.morrigan.model.media.internal.db.LocalMediaDbFactory;
import com.vaguehope.morrigan.model.media.internal.db.LocalMediaDbHelper;
import com.vaguehope.morrigan.model.media.internal.db.LocalMediaDbUpdateTask;
import com.vaguehope.morrigan.model.media.internal.db.MediaSqliteLayerFactory;
import com.vaguehope.morrigan.model.media.internal.db.RemoteMediaDbUpdateTask;
import com.vaguehope.morrigan.model.media.internal.db.SyncMetadataRemoteToLocalTask;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbFactory;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbHelper;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.MorriganTask;

public class MediaFactoryImpl implements MediaFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Config config;
	private final LocalMediaDbUpdateTask.Factory localMixedMediaDbUpdateTaskFactory;

	public MediaFactoryImpl (final Config config, final PlaybackEngineFactory playbackEngineFactoryTracker) {
		this.config = config;
		this.localMixedMediaDbUpdateTaskFactory = new LocalMediaDbUpdateTask.Factory(playbackEngineFactoryTracker, this);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Map<String, MediaDb> addedLocals = new ConcurrentHashMap<>();

	@Override
	public Collection<MediaListReference> getAllLocalMixedMediaDbs () {
		final List<MediaListReference> real = LocalMediaDbHelper.getAllMmdb(this.config);
		if (this.addedLocals.size() < 1) return real;

		final Collection<MediaListReference> ret = new ArrayList<>();
		ret.addAll(real);
		for (final MediaDb db : this.addedLocals.values()) {
			ret.add(new MediaListReferenceImpl(MediaListType.LOCALMMDB, db.getListId(), db.getListName(), false));
		}
		return ret;
	}

	@Override
	public MediaDb createLocalMixedMediaDb (final String name) throws MorriganException {
		return LocalMediaDbHelper.createMmdb(this.config, name);
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
	public MediaDb getLocalMixedMediaDbBySerial (final String serial) throws DbException {
		return LocalMediaDbFactory.getMainBySerial(serial);
	}

	/**
	 * Examples of MID:
	 * mid=LOCALMMDB/test.local.db3/query/*
	 * mid=EXTMMDB/abcdefgh-927d-f5c4-ffff-ijklmnopqrst/query/*
	 * mid=EXTMMDB/abcdefgh-444c-164e-9d41-ijklmnopqrst/query/id%3D0
	 * mid=EXTMMDB/abcdefgh-927d-f5c4-ffff-ijklmnopqrst/query/id%3D1fcee07abcdefghiujklmnopqrstuvwxyz810c6e-foo_bar
	 */
	@Override
	public MediaList getMediaListByMid(final String mid, final String filter) throws DbException, MorriganException {
		final String[] parts = mid.split(":|/");
		if (parts.length < 2) throw new IllegalArgumentException("Invalid MID: " + mid);

		final String type = parts[0];
		final String name = parts[1];

		if (type.equals(MediaListType.LOCALMMDB.toString())) {
			final MediaDb local = this.addedLocals.get(StringUtils.removeEndIgnoreCase(name, Config.MMDB_LOCAL_FILE_EXT));
			if (local != null) return local;

			final String f = LocalMediaDbHelper.getFullPathToMmdb(this.config, name);
			return getLocalMixedMediaDb(f, filter);
		}
		else if (type.equals(MediaListType.REMOTEMMDB.toString())) {
			final String f = RemoteMixedMediaDbHelper.getFullPathToMmdb(this.config, name);
			return RemoteMixedMediaDbFactory.getExisting(f, filter);
		}
		else if (type.equals(MediaListType.EXTMMDB.toString())) {
			return getExternalList(name, filter);
		}
		throw new IllegalArgumentException("Invalid MID: " + mid);
	}

	@Override
	public MediaList getMediaListByRef(final MediaListReference ref) throws DbException, MorriganException {
		return getMediaListByMid(ref.getMid(), null);
	}

	@Override
	public MediaList getMediaListByRef(final MediaListReference ref, final String filter) throws DbException, MorriganException {
		return getMediaListByMid(ref.getMid(), filter);
	}

	/**
	 * For testing use only.  Can use to add in mock implementations.
	 */
	public void addLocalMixedMediaDb (final MediaDb db) {
		this.addedLocals.put(db.getListId(), db);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public MediaDb getLocalMixedMediaDbTransactional (final MediaDb lmmdb) throws DbException {
		return LocalMediaDbFactory.getTransactional(lmmdb.getDbPath());
	}


	@Override
	public MediaDb getMediaItemDbTransactional (final MediaDb db) throws DbException {
		if (MediaListType.LOCALMMDB.toString().equals(db.getType())) {
			return LocalMediaDbFactory.getTransactional(db.getDbPath());
		}
		throw new IllegalArgumentException("Can't create transactional connection to DB of type '" + db.getType() + "'.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public Collection<MediaListReference> getAllRemoteMixedMediaDbs () {
		throw new IllegalArgumentException("See server package.");
	}

	@Override
	public RemoteMediaDb createRemoteMixedMediaDb (final String mmdbUrl) {
		throw new IllegalArgumentException("See server package.");
	}

	@Override
	public RemoteMediaDb getRemoteMixedMediaDb (final String dbName) {
		throw new IllegalArgumentException("See server package.");
	}

	@Override
	public RemoteMediaDb getRemoteMixedMediaDb (final String dbName, final URL url) {
		throw new IllegalArgumentException("See server package.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public MediaStorageLayer getStorageLayerWithNewItemFactory(final String filepath) throws DbException {
		final DefaultMediaItemFactory itemFactory = new DefaultMediaItemFactory(null);
		return MediaSqliteLayerFactory.getTransactional(filepath, itemFactory);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Map<String, MediaList> externalListsByListId = new ConcurrentSkipListMap<>();

	@Override
	public Collection<MediaListReference> getExternalLists () {
		final List<MediaListReference> ret = new ArrayList<>();
		for (final MediaList list : this.externalListsByListId.values()) {
			ret.add(new MediaListReferenceImpl(MediaListType.EXTMMDB, list.getListId(), list.getListName(), list.hasNodes()));
		}
		return ret;
	}

	@Override
	public MediaList getExternalListBySerial(final String serial) {
		// FIXME this is slow and brittle, eg if json serials are formatted differently.
		for (final MediaList list : this.externalListsByListId.values()) {
			if (serial.equals(list.getSerial())) return list;
		}
		return null;
	}

	@Override
	public MediaList getExternalList (final String id, final String filter) throws MorriganException {
		MediaList list = this.externalListsByListId.get(id);
		if (list == null) return null;

		if (StringUtils.isBlank(filter)) return list;
		return list.makeView(filter);
	}

	@Override
	public void addExternalList (final MediaList db) {
		this.externalListsByListId.put(db.getListId(), db);
	}

	@Override
	public MediaList removeExternalList (final String id) {
		return this.externalListsByListId.remove(id);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public DurationData getNewDurationData (final long duration, final boolean complete) {
		return new DurationDataImpl(duration, complete);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public MorriganTask getLocalMixedMediaDbUpdateTask (final MediaDb library) {
		return this.localMixedMediaDbUpdateTaskFactory.manufacture(library);
	}

	@Override
	public MorriganTask getRemoteMixedMediaDbUpdateTask (final RemoteMediaDb library) {
		return RemoteMediaDbUpdateTask.FACTORY.manufacture(library, null);
	}

	@Override
	public MorriganTask getMediaFileCopyTask (final MediaList mediaItemList, final List<MediaItem> mediaSelection, final File targetDirectory) {
		return new MediaFileCopyTask(mediaItemList, mediaSelection, targetDirectory);
	}

	@Override
	public MorriganTask getNewCopyToLocalMmdbTask (final MediaList fromList, final Collection<MediaItem> itemsToCopy, final MediaDb toDb) {
		return new CopyToLocalMmdbTask(fromList, itemsToCopy, toDb, this.config);
	}

	@Override
	public MorriganTask getSyncMetadataRemoteToLocalTask (final MediaDb local, final RemoteMediaDb remote) {
		// TODO FIXME use a factory to prevent duplicates.
		return new SyncMetadataRemoteToLocalTask(local, remote, this);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void readTrackTags (final MediaDb itemDb, final MediaItem mt, final File file) throws IOException, MorriganException {
		try {
			TrackTagHelper.readTrackTags(itemDb, mt, file);
		}
		catch (final TagException e) {
			throw new MorriganException(e);
		}
		catch (final ReadOnlyFileException e) {
			throw new MorriganException(e);
		}
		catch (final InvalidAudioFrameException e) {
			throw new MorriganException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
