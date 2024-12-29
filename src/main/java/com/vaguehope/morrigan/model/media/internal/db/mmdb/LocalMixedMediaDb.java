package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;

public class LocalMixedMediaDb extends AbstractMixedMediaDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected LocalMixedMediaDb (final String listName, final MediaItemDbConfig config, final IMediaItemStorageLayer dbLayer) {
		super(listName, config, dbLayer);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public MediaListType getType () {
		return MediaListType.LOCALMMDB;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
