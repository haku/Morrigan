package net.sparktank.morrigan.model.media.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.DurationData;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaItem;
import net.sparktank.morrigan.model.media.IMediaItemDb;
import net.sparktank.morrigan.model.media.IMediaItemList;
import net.sparktank.morrigan.model.media.IMediaPlaylist;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.MediaFactory;
import net.sparktank.morrigan.model.media.MediaListReference;
import net.sparktank.morrigan.model.media.MediaTagType;
import net.sparktank.morrigan.model.media.internal.CopyToLocalMmdbTask;
import net.sparktank.morrigan.model.media.internal.DurationDataImpl;
import net.sparktank.morrigan.model.media.internal.LocalMixedMediaDb;
import net.sparktank.morrigan.model.media.internal.LocalMixedMediaDbHelper;
import net.sparktank.morrigan.model.media.internal.LocalMixedMediaDbUpdateTask;
import net.sparktank.morrigan.model.media.internal.MediaFileCopyTask;
import net.sparktank.morrigan.model.media.internal.MediaItemDb;
import net.sparktank.morrigan.model.media.internal.MediaPlaylist;
import net.sparktank.morrigan.model.media.internal.MediaTagTypeImpl;
import net.sparktank.morrigan.model.media.internal.MediaTrack;
import net.sparktank.morrigan.model.media.internal.MixedMediaItem;
import net.sparktank.morrigan.model.media.internal.PlaylistHelper;
import net.sparktank.morrigan.model.media.internal.RemoteMixedMediaDbUpdateTask;
import net.sparktank.morrigan.model.media.internal.TrackTagHelper;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.sqlitewrapper.DbException;

import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

public class MediaFactoryImpl implements MediaFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final static MediaFactory INSTANCE = new MediaFactoryImpl();
	
	public static MediaFactory get () {
		return INSTANCE;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public Collection<MediaListReference> getAllLocalMixedMediaDbs() {
		return LocalMixedMediaDbHelper.getAllMmdb();
	}
	
	@Override
	public ILocalMixedMediaDb createLocalMixedMediaDb(String name) throws MorriganException {
		return LocalMixedMediaDbHelper.createMmdb(name);
	}
	
	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDb(String libraryName) throws DbException {
		return LocalMixedMediaDb.LOCAL_MMDB_FACTORY.manufacture(libraryName);
	}
	
	/**
	 * // FIXME This will not work ATM.
	 */
	@Override
	public ILocalMixedMediaDb getLocalMixedMediaDb(String libraryName, String searchTerm) throws DbException {
		return LocalMixedMediaDb.LOCAL_MMDB_FACTORY.manufacture(libraryName, searchTerm);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public Collection<MediaListReference> getAllRemoteMixedMediaDbs() {
		throw new IllegalArgumentException("See server package.");
	}
	
	@Override
	public IRemoteMixedMediaDb createRemoteMixedMediaDb(String mmdbUrl) {
		throw new IllegalArgumentException("See server package.");
	}
	
	@Override
	public IRemoteMixedMediaDb getRemoteMixedMediaDb(String dbName) {
		throw new IllegalArgumentException("See server package.");
	}
	
	@Override
	public IRemoteMixedMediaDb getRemoteMixedMediaDb(String dbName, URL url) {
		throw new IllegalArgumentException("See server package.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public IMediaPlaylist createPlaylist (String plName) throws MorriganException {
		return PlaylistHelper.createPl(plName);
	}
	
	@Override
	public IMediaPlaylist getPlaylist(String filePath) throws MorriganException {
		return MediaPlaylist.FACTORY.manufacture(filePath);
	}
	
	@Override
	public IMediaTrack getNewMediaTrack(String filePath) {
		return new MediaTrack(filePath);
	}
	
	@Override
	public void disposeAllPlaylists() {
		MediaPlaylist.FACTORY.disposeAll();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public IMixedMediaItem getMixedMediaItem() {
		return new MixedMediaItem();
	}
	
	@Override
	public IMixedMediaItem getMixedMediaItem(MediaType type) {
		return new MixedMediaItem(type);
	}
	
	@Override
	public IMixedMediaItem getMixedMediaItem(String filePath) {
		return new MixedMediaItem(filePath);
	}
	
	@Override
	public IMixedMediaItem getMixedMediaItem(MediaType type, String filePath) {
		return new MixedMediaItem(type, filePath);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public DurationData getNewDurationData(long duration, boolean complete) {
		return new DurationDataImpl(duration, complete);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public IMorriganTask getLocalMixedMediaDbUpdateTask(ILocalMixedMediaDb library) {
		return LocalMixedMediaDbUpdateTask.FACTORY.manufacture(library);
	}
	
	@Override
	public IMorriganTask getRemoteMixedMediaDbUpdateTask(IRemoteMixedMediaDb library) {
		return RemoteMixedMediaDbUpdateTask.FACTORY.manufacture(library);
	}
	
	@Override
	public <T extends IMediaItem> IMorriganTask getMediaFileCopyTask(IMediaItemList<T> mediaItemList, List<T> mediaSelection, File targetDirectory) {
		return new MediaFileCopyTask<T>(mediaItemList, mediaSelection, targetDirectory);
	}
	
	@Override
	public <T extends IMediaItem> IMorriganTask getNewCopyToLocalMmdbTask(IMediaItemList<T> fromList, Collection<T> itemsToCopy, ILocalMixedMediaDb toDb) {
		return new CopyToLocalMmdbTask<T>(fromList, itemsToCopy, toDb);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public MediaTagType getMediaTagTypeManual() {
		return MediaTagTypeImpl.MANUAL;
	}
	
	@Override
	public MediaTagType getMediaTagTypeAutomatic() {
		return MediaTagTypeImpl.AUTOMATIC;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void readTrackTags(IMediaItemDb<?, ?, ?> itemDb, IMediaTrack mlt, File file) throws IOException, MorriganException {
		try {
			TrackTagHelper.readTrackTags(itemDb, mlt, file);
		}
		catch (TagException e) {
			throw new MorriganException(e);
		} catch (ReadOnlyFileException e) {
			throw new MorriganException(e);
		} catch (InvalidAudioFrameException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public String escapeSearch(String term) {
		return MediaItemDb.escapeSearch(term);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
