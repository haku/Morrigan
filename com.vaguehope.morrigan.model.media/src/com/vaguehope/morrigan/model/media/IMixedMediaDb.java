package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.sqlitewrapper.DbException;

public interface IMixedMediaDb
		extends
		IMixedMediaList<IMixedMediaItem>,
		IMediaTrackDb<IMixedMediaStorageLayer, IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void setDefaultMediaType (MediaType mediaType) throws MorriganException;

	void setDefaultMediaType (MediaType mediaType, boolean saveToDb) throws MorriganException;

	MediaType getDefaultMediaType ();

	List<IMixedMediaItem> simpleSearchMedia (MediaType mediaType, String term, int maxResults) throws DbException;
	List<IMixedMediaItem> simpleSearchMedia (MediaType mediaType, String term, int maxResults, IDbColumn[] sortColumns, SortDirection[] sortDirections, boolean includeDisabled) throws DbException;

	Collection<IMixedMediaItem> getAlbumItems (MediaType mediaType, MediaAlbum album) throws MorriganException;
	/**
	 * @return File path to cover art or null.
	 */
	File findAlbumCoverArt(MediaAlbum album) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
