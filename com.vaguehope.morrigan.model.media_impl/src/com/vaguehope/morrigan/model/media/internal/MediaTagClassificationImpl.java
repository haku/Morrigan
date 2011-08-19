package com.vaguehope.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.media.MediaTagClassification;


public class MediaTagClassificationImpl implements MediaTagClassification {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long rowId;
	private String classification;
	
	public MediaTagClassificationImpl (long rowId, String classification) {
		this.rowId = rowId;
		this.classification = classification;
	}
	
	@Override
	public long getDbRowId() {
		return this.rowId;
	}
	
	@Override
	public boolean setDbRowId(long dbRowId) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getClassification() {
		return this.classification;
	}
	@Override
	public void setClassification(String classification) {
		this.classification = classification;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString() {
		return getClassification();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Equivalence methods based purely on getRowId().
	
	@Override
	public boolean equals(Object aThat) {
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
