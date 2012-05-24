package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactoryTracker;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.CopyToLocalMmdbTask;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDbFactory;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDbHelper;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDbUpdateTask;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.RemoteMixedMediaDbUpdateTask;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.SyncMetadataRemoteToLocalTask;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.sqlitewrapper.DbException;

public class MediaFactoryImpl implements MediaFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	LocalMixedMediaDbUpdateTask.Factory localMixedMediaDbUpdateTaskFactory;

	public MediaFactoryImpl (PlaybackEngineFactoryTracker playbackEngineFactoryTracker) {
		this.localMixedMediaDbUpdateTaskFactory = new LocalMixedMediaDbUpdateTask.Factory(playbackEngineFactoryTracker, this);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public Collection<MediaListReference> getAllLocalMixedMediaDbs () {
		return LocalMixedMediaDbHelper.getAllMmdb();
	}

	@Override
	public ILocalMixedMediaDb createLocalMixedMediaDb (String name) throws MorriganException {
		return LocalMixedMediaDbHelper.createMmdb(name);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDb (String fullFilePath) throws DbException {
		return LocalMixedMediaDbFactory.getMain(fullFilePath);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDb (String fullFilePath, String filter) throws DbException {
		return LocalMixedMediaDbFactory.getView(fullFilePath, filter);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDbBySerial (String serial) throws DbException {
		return LocalMixedMediaDbFactory.getMainBySerial(serial);
	}

	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDbTransactional (ILocalMixedMediaDb lmmdb) throws DbException {
		return LocalMixedMediaDbFactory.getTransactional(lmmdb.getDbPath());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public IMediaItemDb<?, ?> getMediaItemDbTransactional (IMediaItemDb<?, ?> db) throws DbException {
		if (ILocalMixedMediaDb.TYPE.equals(db.getType())) {
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
	public IRemoteMixedMediaDb createRemoteMixedMediaDb (String mmdbUrl) {
		throw new IllegalArgumentException("See server package.");
	}

	@Override
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName) {
		throw new IllegalArgumentException("See server package.");
	}

	@Override
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName, URL url) {
		throw new IllegalArgumentException("See server package.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public DurationData getNewDurationData (long duration, boolean complete) {
		return new DurationDataImpl(duration, complete);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public MorriganTask getLocalMixedMediaDbUpdateTask (ILocalMixedMediaDb library) {
		return this.localMixedMediaDbUpdateTaskFactory.manufacture(library);
	}

	@Override
	public MorriganTask getRemoteMixedMediaDbUpdateTask (IRemoteMixedMediaDb library) {
		return RemoteMixedMediaDbUpdateTask.FACTORY.manufacture(library);
	}

	@Override
	public <T extends IMediaItem> MorriganTask getMediaFileCopyTask (IMediaItemList<T> mediaItemList, List<T> mediaSelection, File targetDirectory) {
		return new MediaFileCopyTask<T>(mediaItemList, mediaSelection, targetDirectory);
	}

	@Override
	public <T extends IMediaItem> MorriganTask getNewCopyToLocalMmdbTask (IMediaItemList<T> fromList, Collection<T> itemsToCopy, ILocalMixedMediaDb toDb) {
		return new CopyToLocalMmdbTask<T>(fromList, itemsToCopy, toDb);
	}

	@Override
	public MorriganTask getSyncMetadataRemoteToLocalTask (ILocalMixedMediaDb local, IRemoteMixedMediaDb remote) {
		// TODO FIXME use a factory to prevent duplicates.
		return new SyncMetadataRemoteToLocalTask(local, remote, this);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void readTrackTags (IMediaItemDb<?, ?> itemDb, IMediaTrack mt, File file) throws IOException, MorriganException {
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
