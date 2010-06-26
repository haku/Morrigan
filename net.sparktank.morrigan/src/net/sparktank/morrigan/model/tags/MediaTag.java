package net.sparktank.morrigan.model.tags;

public class MediaTag {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long dbRowId;
	private String tag;
	private MediaTagType type;
	private MediaTagClassification classification;
	
	public MediaTag (long dbRowId, String tag, MediaTagType type) {
		this.dbRowId = dbRowId;
		this.tag = tag;
		this.type = type;
		this.classification = null;
	}
	
	public MediaTag (long dbRowId, String tag, MediaTagType type, MediaTagClassification classification) {
		this.dbRowId = dbRowId;
		this.tag = tag;
		this.type = type;
		this.classification = classification;
	}
	
	public long getDbRowId() {
		return dbRowId;
	}
	
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public MediaTagType getType() {
		return type;
	}
	public void setType(MediaTagType type) {
		this.type = type;
	}
	
	public MediaTagClassification getClassification() {
		return classification;
	}
	public void setClassification(MediaTagClassification classification) {
		this.classification = classification;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString() {
		return getTag();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
