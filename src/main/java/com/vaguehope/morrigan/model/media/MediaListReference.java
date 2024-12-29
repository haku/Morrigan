package com.vaguehope.morrigan.model.media;

public interface MediaListReference extends Comparable<MediaListReference> {

	public enum MediaListType {LOCALMMDB, REMOTEMMDB, PLAYLIST, EXTMMDB}

	MediaListType getType ();
	String getIdentifier ();
	String getTitle ();
	String getMid();

}
