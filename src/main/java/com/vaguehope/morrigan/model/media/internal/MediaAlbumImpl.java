package com.vaguehope.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.media.MediaAlbum;

public class MediaAlbumImpl implements MediaAlbum {

	private final long dbRowId;
	private final String name;
	private final int trackCount;

	public MediaAlbumImpl (final long dbRowId, final String name, final int trackCount) {
		this.dbRowId = dbRowId;
		this.name = name;
		this.trackCount = trackCount;
	}

	@Override
	public long getDbRowId () {
		return this.dbRowId;
	}

	@Override
	public boolean setDbRowId (final long dbRowId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName () {
		return this.name;
	}

	@Override
	public int getTrackCount () {
		return this.trackCount;
	}

}
