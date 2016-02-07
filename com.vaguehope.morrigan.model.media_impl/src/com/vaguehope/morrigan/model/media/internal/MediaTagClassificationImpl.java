package com.vaguehope.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.media.MediaTagClassification;


public class MediaTagClassificationImpl implements MediaTagClassification {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final long rowId;
	private final String classification;

	public MediaTagClassificationImpl (final long rowId, final String classification) {
		this.rowId = rowId;
		this.classification = classification;
	}

	@Override
	public long getDbRowId() {
		return this.rowId;
	}

	@Override
	public boolean setDbRowId(final long dbRowId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getClassification() {
		return this.classification;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String toString() {
		return getClassification();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Equivalence methods based purely on getRowId().

	@Override
	public boolean equals(final Object aThat) {
		if ( aThat == null ) return false;
		if ( this == aThat ) return true;
		if ( !(aThat instanceof MediaTagClassificationImpl) ) return false;
		MediaTagClassificationImpl that = (MediaTagClassificationImpl)aThat;

		return this.getDbRowId() == that.getDbRowId();
	}

	@Override
	public int hashCode() {
		return  (int)(this.getDbRowId()^(this.getDbRowId()>>>32));
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
