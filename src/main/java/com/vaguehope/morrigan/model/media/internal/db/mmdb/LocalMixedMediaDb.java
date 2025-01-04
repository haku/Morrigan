package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;

public class LocalMixedMediaDb extends AbstractMixedMediaDb {

	protected LocalMixedMediaDb (final String listName, final MediaItemDbConfig config, final IMediaItemStorageLayer dbLayer) {
		super(listName, config, dbLayer);
	}

	@Override
	public MediaListType getType () {
		return MediaListType.LOCALMMDB;
	}

	@Override
	public boolean canMakeView() {
		return true;
	}

	@Override
	public IMediaItemList makeView(String filter) throws MorriganException {
		return LocalMixedMediaDbFactory.getView(getDbPath(), filter);
	}

}
