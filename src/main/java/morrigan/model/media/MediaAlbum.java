package morrigan.model.media;

import morrigan.model.db.IDbItem;

public interface MediaAlbum extends IDbItem {

	String getName();

	int getTrackCount ();

}
