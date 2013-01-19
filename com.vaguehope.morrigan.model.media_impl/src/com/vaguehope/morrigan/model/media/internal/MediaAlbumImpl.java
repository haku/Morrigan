package com.vaguehope.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.media.MediaAlbum;

public class MediaAlbumImpl implements MediaAlbum {

	private final long dbRowId;
	private final String name;

	public MediaAlbumImpl (long dbRowId, String name) {
		this.dbRowId = dbRowId;
		this.name = name;
	}

	@Override
	public long getDbRowId () {
		return this.dbRowId;
	}

	@Override
	public boolean setDbRowId (long dbRowId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName () {
		return this.name;
	}

}
