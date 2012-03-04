package com.vaguehope.morrigan.model.media;

public interface MediaListReference extends Comparable<MediaListReference> {

	enum MediaListType {LOCALMMDB, REMOTEMMDB, PLAYLIST}

	MediaListType getType ();
	String getIdentifier ();
	String getTitle ();

}
