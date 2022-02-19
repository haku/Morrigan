package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaStorageLayer;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;

public class LocalMixedMediaDb extends AbstractMixedMediaDb implements ILocalMixedMediaDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected LocalMixedMediaDb (final String listName, final MediaItemDbConfig config, final IMixedMediaStorageLayer dbLayer) {
		super(listName, config, dbLayer);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getType () {
		return MediaListType.LOCALMMDB.toString();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
