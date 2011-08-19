package com.vaguehope.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.media.MediaListReference;

public class MediaListReferenceImpl implements MediaListReference {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaListType type;
    private final String identifier;
    private final String title;
    
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    
	public MediaListReferenceImpl (MediaListType type, String identifier, String title) {
		this.type = type;
		this.identifier = identifier;
		this.title = title;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public MediaListType getType() {
		return this.type;
	}
	
	@Override
	public String getIdentifier() {
		return this.identifier;
	}
	
	@Override
	public String getTitle() {
		return this.title;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString () {
		return this.title;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public int compareTo (MediaListReference o) {
		return this.toString().compareTo(o.toString());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
