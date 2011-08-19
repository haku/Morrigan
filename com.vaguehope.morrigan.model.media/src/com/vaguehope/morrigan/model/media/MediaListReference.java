package com.vaguehope.morrigan.model.media;

public interface MediaListReference extends Comparable<MediaListReference> {
	
	public enum MediaListType {LOCALMMDB, REMOTEMMDB, PLAYLIST}
	
	public MediaListType getType ();
	public String getIdentifier ();
	public String getTitle ();
	
}
