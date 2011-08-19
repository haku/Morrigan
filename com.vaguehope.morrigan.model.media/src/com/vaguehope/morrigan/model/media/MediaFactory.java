package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.tasks.IMorriganTask;
import com.vaguehope.sqlitewrapper.DbException;


/**
 * TODO remove remains of play-lists - they don't even work anyway.
 */
public interface MediaFactory {
	
	/**
	 * List them all.
	 */
	public Collection<MediaListReference> getAllLocalMixedMediaDbs ();
	
	/**
	 * Create a new one.
	 */
	public ILocalMixedMediaDb createLocalMixedMediaDb (String name) throws MorriganException;
	
	/**
	 * Basic impl.
	 */
	public ILocalMixedMediaDb getLocalMixedMediaDb (String fullFilePath) throws DbException;
	
	/**
	 * Impl with a filter set.
	 */
	public ILocalMixedMediaDb getLocalMixedMediaDb (String fullFilePath, String searchTerm) throws DbException;
	
	public ILocalMixedMediaDb getLocalMixedMediaDbBySerial (String serial) throws DbException;
	
	public Collection<MediaListReference> getAllRemoteMixedMediaDbs ();
	public IRemoteMixedMediaDb createRemoteMixedMediaDb (String mmdbUrl);
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName);
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName, URL url);
	
	public IMediaPlaylist createPlaylist (String plName) throws MorriganException;
	public IMediaPlaylist getPlaylist (String filePath) throws MorriganException;
	public void disposeAllPlaylists ();
	
	public DurationData getNewDurationData (long duration, boolean complete);
	
	/*
	 * TODO merge these next two methods?
	 */
	public IMorriganTask getLocalMixedMediaDbUpdateTask (ILocalMixedMediaDb library);
	public IMorriganTask getRemoteMixedMediaDbUpdateTask (IRemoteMixedMediaDb library);
	public <T extends IMediaItem> IMorriganTask getMediaFileCopyTask (IMediaItemList<T> mediaItemList, List<T> mediaSelection, File targetDirectory);
	public <T extends IMediaItem> IMorriganTask getNewCopyToLocalMmdbTask (IMediaItemList<T> fromList, Collection<T> itemsToCopy, ILocalMixedMediaDb toDb);
	public IMorriganTask getSyncMetadataRemoteToLocalTask (ILocalMixedMediaDb local, IRemoteMixedMediaDb remote);
	
	public void readTrackTags (IMediaItemDb<?,?,?> itemDb, IMediaTrack mlt, File file) throws IOException, MorriganException;
	
}
