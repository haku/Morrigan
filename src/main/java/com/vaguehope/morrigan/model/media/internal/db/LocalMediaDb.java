package com.vaguehope.morrigan.model.media.internal.db;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;

public class LocalMediaDb extends AbstractMediaDb {

	protected LocalMediaDb (final String listName, final MediaDbConfig config) {
		super(listName, config);
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
	public MediaList makeView(String filter) throws MorriganException {
		return LocalMediaDbFactory.getView(getDbPath(), filter);
	}

}
