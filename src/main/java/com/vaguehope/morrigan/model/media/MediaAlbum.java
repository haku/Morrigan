package com.vaguehope.morrigan.model.media;

import com.vaguehope.morrigan.model.db.IDbItem;

public interface MediaAlbum extends IDbItem {

	String getName();

	int getTrackCount ();

}
