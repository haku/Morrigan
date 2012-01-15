package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.tasks.IMorriganTask;
import com.vaguehope.sqlitewrapper.DbException;


/**
 * TODO remove remains of play-lists - they don't even work anyway.
 */
public interface MediaFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * List them all.
	 */
	public Collection<MediaListReference> getAllLocalMixedMediaDbs ();
	
	/**
	 * Create a new one.
	 */
	public ILocalMixedMediaDb createLocalMixedMediaDb (String name) throws MorriganException;
	
	/**
	 * Main instance.
	 */
	public ILocalMixedMediaDb getLocalMixedMediaDb (String fullFilePath) throws DbException;
	
	/**
	 * With a filter set: a view.
	 */
	public ILocalMixedMediaDb getLocalMixedMediaDb (String fullFilePath, String searchTerm) throws DbException;
	
	/**
	 * Same as getLocalMixedMediaDb().
	 */
	public ILocalMixedMediaDb getLocalMixedMediaDbBySerial (String serial) throws DbException;
	
	/**
	 * Create an new instance with transactional behaviour.
	 */
	public ILocalMixedMediaDb getLocalMixedMediaDbTransactional (ILocalMixedMediaDb lmmdb) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public IMediaItemDb<?,?> getMediaItemDbTransactional (IMediaItemDb<?,?> db) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public Collection<MediaListReference> getAllRemoteMixedMediaDbs ();
	public IRemoteMixedMediaDb createRemoteMixedMediaDb (String mmdbUrl);
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName);
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName, URL url);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public DurationData getNewDurationData (long duration, boolean complete);
	
	/*
	 * TODO merge these next two methods?
	 */
	public IMorriganTask getLocalMixedMediaDbUpdateTask (ILocalMixedMediaDb library);
	public IMorriganTask getRemoteMixedMediaDbUpdateTask (IRemoteMixedMediaDb library);
	public <T extends IMediaItem> IMorriganTask getMediaFileCopyTask (IMediaItemList<T> mediaItemList, List<T> mediaSelection, File targetDirectory);
	public <T extends IMediaItem> IMorriganTask getNewCopyToLocalMmdbTask (IMediaItemList<T> fromList, Collection<T> itemsToCopy, ILocalMixedMediaDb toDb);
	public IMorriganTask getSyncMetadataRemoteToLocalTask (ILocalMixedMediaDb local, IRemoteMixedMediaDb remote);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void readTrackTags (IMediaItemDb<?,?> itemDb, IMediaTrack mlt, File file) throws IOException, MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
