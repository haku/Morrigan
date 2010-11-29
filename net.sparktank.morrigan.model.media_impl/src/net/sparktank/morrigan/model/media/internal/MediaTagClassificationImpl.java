package net.sparktank.morrigan.model.media.internal;

import net.sparktank.morrigan.model.media.MediaTagClassification;


public class MediaTagClassificationImpl implements MediaTagClassification { // TODO add interface.
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long rowId;
	private String classification;
	
	public MediaTagClassificationImpl (long rowId, String classification) {
		this.rowId = rowId;
		this.classification = classification;
	}
	
	@Override
	public long getRowId() {
		return this.rowId;
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
		
		return this.getRowId() == that.getRowId();
	}
	
	@Override
	public int hashCode() {
		return  (int)(this.getRowId()^(this.getRowId()>>>32));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
