package com.vaguehope.morrigan.model.media;

import java.util.List;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.sqlitewrapper.DbException;

public interface IMixedMediaDb
		extends
		IMixedMediaList<IMixedMediaItem>,
		IMediaTrackDb<IMixedMediaStorageLayer<IMixedMediaItem>, IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void setDefaultMediaType (MediaType mediaType) throws MorriganException;

	void setDefaultMediaType (MediaType mediaType, boolean saveToDb) throws MorriganException;

	MediaType getDefaultMediaType ();

	List<IMixedMediaItem> simpleSearchMedia (MediaType mediaType, String term, int maxResults) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
