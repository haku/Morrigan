package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaStorageLayer;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;


public class LocalMixedMediaDb extends AbstractMixedMediaDb implements ILocalMixedMediaDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected LocalMixedMediaDb (String listName, MediaItemDbConfig config, IMixedMediaStorageLayer dbLayer) {
		super(listName, config, dbLayer);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getType () {
		return TYPE;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
