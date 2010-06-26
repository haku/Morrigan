package net.sparktank.morrigan.model.tags;

public class MediaTagClassification {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long rowId;
	private String classification;
	
	public MediaTagClassification (long rowId, String classification) {
		this.rowId = rowId;
		this.classification = classification;
	}
	
	public long getRowId() {
		return rowId;
	}
	
	public String getClassification() {
		return classification;
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
}
