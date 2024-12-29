package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public interface IMixedMediaDb extends IMediaItemDb<IMixedMediaStorageLayer> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void setDefaultMediaType (MediaType mediaType) throws MorriganException;

	void setDefaultMediaType (MediaType mediaType, boolean saveToDb) throws MorriganException;

	MediaType getDefaultMediaType ();

	List<IMediaItem> simpleSearchMedia (MediaType mediaType, String term, int maxResults) throws DbException;
	List<IMediaItem> simpleSearchMedia (MediaType mediaType, String term, int maxResults, IDbColumn[] sortColumns, SortDirection[] sortDirections, boolean includeDisabled) throws DbException;

	Collection<IMediaItem> getAlbumItems (MediaType mediaType, MediaAlbum album) throws MorriganException;
	/**
	 * @return File path to cover art or null.
	 */
	File findAlbumCoverArt(MediaAlbum album) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
