package net.sparktank.morrigan.model.tags;

public class MediaTag {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String tag;
	private MediaTagType type;
	private MediaTagClassification classification;
	
	public MediaTag (String tag, MediaTagType type) {
		this.tag = tag;
		this.type = type;
		this.classification = null;
	}
	
	public MediaTag (String tag, MediaTagType type, MediaTagClassification classification) {
		this.tag = tag;
		this.type = type;
		this.classification = classification;
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
}
