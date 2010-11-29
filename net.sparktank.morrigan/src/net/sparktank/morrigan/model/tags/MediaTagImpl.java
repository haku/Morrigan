package net.sparktank.morrigan.model.tags;

public class MediaTagImpl {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long dbRowId;
	private String tag;
	private MediaTagTypeImpl type;
	private MediaTagClassificationImpl classification;
	
	public MediaTagImpl (long dbRowId, String tag, MediaTagTypeImpl type) {
		this.dbRowId = dbRowId;
		this.tag = tag;
		this.type = type;
		this.classification = null;
	}
	
	public MediaTagImpl (long dbRowId, String tag, MediaTagTypeImpl type, MediaTagClassificationImpl classification) {
		this.dbRowId = dbRowId;
		this.tag = tag;
		this.type = type;
		this.classification = classification;
	}
	
	public long getDbRowId() {
		return this.dbRowId;
	}
	
	public String getTag() {
		return this.tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public MediaTagTypeImpl getType() {
		return this.type;
	}
	public void setType(MediaTagTypeImpl type) {
		this.type = type;
	}
	
	public MediaTagClassificationImpl getClassification() {
		return this.classification;
	}
	public void setClassification(MediaTagClassificationImpl classification) {
		this.classification = classification;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getTag());
		
		if (getClassification() != null && getClassification().getClassification().length() > 0) {
			sb.append(" <");
			sb.append(getClassification());
			sb.append(">");
		}
		
		sb.append(" [");
		sb.append(getType().getShortName());
		sb.append("]");
		
		return sb.toString();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
