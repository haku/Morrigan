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
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaStorageLayer;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.CopyToLocalMmdbTask;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDbFactory;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDbHelper;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDbUpdateTask;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.MixedMediaSqliteLayerFactory;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.RemoteMixedMediaDbUpdateTask;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.SyncMetadataRemoteToLocalTask;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbFactory;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbHelper;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.MorriganTask;

public class MediaFactoryImpl implements MediaFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Config config;
	private final LocalMixedMediaDbUpdateTask.Factory localMixedMediaDbUpdateTaskFactory;

	public MediaFactoryImpl (final Config config, final PlaybackEngineFactory playbackEngineFactoryTracker) {
		this.config = config;
		this.localMixedMediaDbUpdateTaskFactory = new LocalMixedMediaDbUpdateTask.Factory(playbackEngineFactoryTracker, this);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Map<String, ILocalMixedMediaDb> addedLocals = new ConcurrentHashMap<>();

	@Override
	public Collection<MediaListReference> getAllLocalMixedMediaDbs () {
		final List<MediaListReference> real = LocalMixedMediaDbHelper.getAllMmdb(this.config);
		if (this.addedLocals.size() < 1) return real;

		final Collection<MediaListReference> ret = new ArrayList<>();
		ret.addAll(real);
		for (final ILocalMixedMediaDb db : this.addedLocals.values()) {
			ret.add(new MediaListReferenceImpl(MediaListType.LOCALMMDB, db.getListId(), db.getListName()));
		}
		return ret;
	}

	@Override
	public ILocalMixedMediaDb createLocalMixedMediaDb (final String name) throws MorriganException {
		return LocalMixedMediaDbHelper.createMmdb(this.config, name);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDb (final String fullFilePath) throws DbException {
		if (!new File(fullFilePath).isFile()) throw new DbException("File not found: " + fullFilePath);
		return LocalMixedMediaDbFactory.getMain(fullFilePath);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDb (final String fullFilePath, final String filter) throws DbException {
		if (!new File(fullFilePath).isFile()) throw new DbException("File not found: " + fullFilePath);
		if (filter == null || filter.length() < 1) return getLocalMixedMediaDb(fullFilePath);
		return LocalMixedMediaDbFactory.getView(fullFilePath, filter);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDbBySerial (final String serial) throws DbException {
		return LocalMixedMediaDbFactory.getMainBySerial(serial);
	}

	/**
	 * Examples of MID:
	 * mid=LOCALMMDB/test.local.db3/query/*
	 * mid=EXTMMDB/abcdefgh-927d-f5c4-ffff-ijklmnopqrst/query/*
	 * mid=EXTMMDB/abcdefgh-444c-164e-9d41-ijklmnopqrst/query/id%3D0
	 * mid=EXTMMDB/abcdefgh-927d-f5c4-ffff-ijklmnopqrst/query/id%3D1fcee07abcdefghiujklmnopqrstuvwxyz810c6e-foo_bar
	 */
	@Override
	public IMixedMediaDb getMixedMediaDbByMid(final String mid, final String filter) throws DbException, MorriganException {
		final String[] parts = mid.split("/");
		if (parts.length < 2) throw new IllegalArgumentException("Invalid MID: " + mid);

		final String type = parts[0];
		final String name = parts[1];

		if (type.equals(MediaListType.LOCALMMDB.toString())) {
			final ILocalMixedMediaDb local = this.addedLocals.get(StringUtils.removeEndIgnoreCase(name, Config.MMDB_LOCAL_FILE_EXT));
			if (local != null) return local;

			final String f = LocalMixedMediaDbHelper.getFullPathToMmdb(this.config, name);
			return getLocalMixedMediaDb(f, filter);
		}
		else if (type.equals(MediaListType.REMOTEMMDB.toString())) {
			final String f = RemoteMixedMediaDbHelper.getFullPathToMmdb(this.config, name);
			return RemoteMixedMediaDbFactory.getExisting(f, filter);
		}
		else if (type.equals(MediaListType.EXTMMDB.toString())) {
			return getExternalDb(name);
		}
		throw new IllegalArgumentException("Invalid MID: " + mid);
	}

	@Override
	public IMixedMediaDb getMixedMediaDbByRef(MediaListReference ref) throws DbException, MorriganException {
		return getMixedMediaDbByMid(ref.getMid(), null);
	}

	@Override
	public IMixedMediaDb getMixedMediaDbByRef(MediaListReference ref, String filter) throws DbException, MorriganException {
		return getMixedMediaDbByMid(ref.getMid(), filter);
	}

	/**
	 * For testing use only.  Can use to add in mock implementations.
	 */
	public void addLocalMixedMediaDb (final ILocalMixedMediaDb db) {
		this.addedLocals.put(db.getListName(), db);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDbTransactional (final ILocalMixedMediaDb lmmdb) throws DbException {
		return LocalMixedMediaDbFactory.getTransactional(lmmdb.getDbPath());
	}


	@Override
	public IMediaItemDb<?, ?> getMediaItemDbTransactional (final IMediaItemDb<?, ?> db) throws DbException {
		if (MediaListType.LOCALMMDB.toString().equals(db.getType())) {
			return LocalMixedMediaDbFactory.getTransactional(db.getDbPath());
		}
		throw new IllegalArgumentException("Can't create transactional connection to DB of type '" + db.getType() + "'.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public Collection<MediaListReference> getAllRemoteMixedMediaDbs () {
		throw new IllegalArgumentException("See server package.");
	}

	@Override
	public IRemoteMixedMediaDb createRemoteMixedMediaDb (final String mmdbUrl) {
		throw new IllegalArgumentException("See server package.");
	}

	@Override
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (final String dbName) {
		throw new IllegalArgumentException("See server package.");
	}

	@Override
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (final String dbName, final URL url) {
		throw new IllegalArgumentException("See server package.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public IMixedMediaStorageLayer getStorageLayer (final String filepath) throws DbException {
		return MixedMediaSqliteLayerFactory.getTransactional(filepath);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Map<String, IMixedMediaDb> externalDbs = new ConcurrentSkipListMap<>();

	@Override
	public Collection<MediaListReference> getExternalDbs () {
		final List<MediaListReference> ret = new ArrayList<>();
		for (IMixedMediaDb db : this.externalDbs.values()) {
			ret.add(new MediaListReferenceImpl(MediaListType.EXTMMDB, db.getListId(), db.getListName()));
		}
		return ret;
	}

	@Override
	public IMixedMediaDb getExternalDb (final String id) {
		return this.externalDbs.get(id);
	}

	@Override
	public void addExternalDb (final IMixedMediaDb db) {
		this.externalDbs.put(db.getListId(), db);
	}

	@Override
	public IMixedMediaDb removeExternalDb (final String id) {
		return this.externalDbs.remove(id);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public DurationData getNewDurationData (final long duration, final boolean complete) {
		return new DurationDataImpl(duration, complete);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public MorriganTask getLocalMixedMediaDbUpdateTask (final ILocalMixedMediaDb library) {
		return this.localMixedMediaDbUpdateTaskFactory.manufacture(library);
	}

	@Override
	public MorriganTask getRemoteMixedMediaDbUpdateTask (final IRemoteMixedMediaDb library) {
		return RemoteMixedMediaDbUpdateTask.FACTORY.manufacture(library);
	}

	@Override
	public <T extends IMediaItem> MorriganTask getMediaFileCopyTask (final IMediaItemList<T> mediaItemList, final List<T> mediaSelection, final File targetDirectory) {
		return new MediaFileCopyTask<>(mediaItemList, mediaSelection, targetDirectory);
	}

	@Override
	public <T extends IMediaItem> MorriganTask getNewCopyToLocalMmdbTask (final IMediaItemList<T> fromList, final Collection<T> itemsToCopy, final ILocalMixedMediaDb toDb) {
		return new CopyToLocalMmdbTask<>(fromList, itemsToCopy, toDb, this.config);
	}

	@Override
	public MorriganTask getSyncMetadataRemoteToLocalTask (final ILocalMixedMediaDb local, final IRemoteMixedMediaDb remote) {
		// TODO FIXME use a factory to prevent duplicates.
		return new SyncMetadataRemoteToLocalTask(local, remote, this);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void readTrackTags (final IMediaItemDb<?, ?> itemDb, final IMediaTrack mt, final File file) throws IOException, MorriganException {
		try {
			TrackTagHelper.readTrackTags(itemDb, mt, file);
		}
		catch (TagException e) {
			throw new MorriganException(e);
		}
		catch (ReadOnlyFileException e) {
			throw new MorriganException(e);
		}
		catch (InvalidAudioFrameException e) {
			throw new MorriganException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
