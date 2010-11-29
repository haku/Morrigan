package net.sparktank.morrigan.model.tags;


public class MediaTagClassificationImpl { // TODO add interface.
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long rowId;
	private String classification;
	
	public MediaTagClassificationImpl (long rowId, String classification) {
		this.rowId = rowId;
		this.classification = classification;
	}
	
	public long getRowId() {
		return this.rowId;
	}
	
	public String getClassification() {
		return this.classification;
	}
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
