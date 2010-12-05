package net.sparktank.morrigan.model.media;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.sqlitewrapper.DbException;

/**
 * TODO replace MediaExplorerItem with MediaListReference (interface).
 * TODO remove remains of play-lists - they don't even work anyway.
 */
public interface MediaFactory {
	
	public Collection<MediaExplorerItem> getAllLocalMixedMediaDbs ();
	public ILocalMixedMediaDb createLocalMixedMediaDb (String name) throws MorriganException;
	public ILocalMixedMediaDb getLocalMixedMediaDb (String libraryName) throws DbException;
	public ILocalMixedMediaDb getLocalMixedMediaDb (String libraryName, String searchTerm) throws DbException;
	
	public Collection<MediaExplorerItem> getAllRemoteMixedMediaDbs ();
	public IRemoteMixedMediaDb createRemoteMixedMediaDb (String mmdbUrl);
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName);
	public IRemoteMixedMediaDb getRemoteMixedMediaDb (String dbName, URL url);
	
	public IMediaPlaylist createPlaylist (String plName) throws MorriganException;
	public IMediaPlaylist getPlaylist (String filePath) throws MorriganException;
	public IMediaTrack getNewMediaTrack (String filePath);
	public void disposeAllPlaylists ();
	
	public IMixedMediaItem getMixedMediaItem ();
	public IMixedMediaItem getMixedMediaItem (MediaType type);
	public IMixedMediaItem getMixedMediaItem (String filePath);
	public IMixedMediaItem getMixedMediaItem (MediaType type, String filePath);
	
	public DurationData getNewDurationData (long duration, boolean complete);
	
	/*
	 * TODO merge these next two methods?
	 */
	public IMorriganTask getLocalMixedMediaDbUpdateTask (ILocalMixedMediaDb library);
	public IMorriganTask getRemoteMixedMediaDbUpdateTask (IRemoteMixedMediaDb library);
	public <T extends IMediaItem> IMorriganTask getMediaFileCopyTask (IMediaItemList<T> mediaItemList, List<T> mediaSelection, File targetDirectory);
	public <T extends IMediaItem> IMorriganTask getNewCopyToLocalMmdbTask (IMediaItemList<T> fromList, Collection<T> itemsToCopy, ILocalMixedMediaDb toDb);
	
	/**
	 * This is a really stupid way to use an enum.. must fix this.
	 */
	public MediaTagType getMediaTagTypeManual ();
	public MediaTagType getMediaTagTypeAutomatic ();
	
	public void readTrackTags (IMediaItemDb<?,?,?> itemDb, IMediaTrack mlt, File file) throws IOException, MorriganException;
	
	/*
	 * TODO fix this unhelpful name.
	 * Merge into functions that do searching so its not exposed?
	 */
	public String escapeSearch (String term);
	
}
