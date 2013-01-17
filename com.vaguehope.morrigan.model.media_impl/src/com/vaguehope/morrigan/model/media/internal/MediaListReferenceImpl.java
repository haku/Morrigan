package com.vaguehope.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.helper.EqualHelper;
import com.vaguehope.morrigan.model.media.MediaListReference;

public class MediaListReferenceImpl implements MediaListReference {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final MediaListType type;
	private final String identifier;
	private final String title;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MediaListReferenceImpl (MediaListType type, String identifier, String title) {
		if (this.type == null) throw new IllegalArgumentException("type is required.");
		if (this.identifier == null) throw new IllegalArgumentException("identifier is required.");
		if (this.title == null) throw new IllegalArgumentException("title is required.");
		this.type = type;
		this.identifier = identifier;
		this.title = title;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public MediaListType getType () {
		return this.type;
	}

	@Override
	public String getIdentifier () {
		return this.identifier;
	}

	@Override
	public String getTitle () {
		return this.title;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String toString () {
		return this.title;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean equals (Object aThat) {
		if (aThat == null) return false;
		if (this == aThat) return true;
		if (!(aThat instanceof MediaListReference)) return false;
		MediaListReference that = (MediaListReference) aThat;
		return EqualHelper.areEqual(this.identifier, that.getIdentifier());
	}

	@Override
	public int hashCode () {
		return this.identifier.hashCode();
	}

	@Override
	public int compareTo (MediaListReference o) {
		return this.toString().compareTo(o.toString());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
