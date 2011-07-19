package net.sparktank.morrigan.model.media.internal;

import net.sparktank.morrigan.model.media.MediaTag;
import net.sparktank.morrigan.model.media.MediaTagClassification;
import net.sparktank.morrigan.model.media.MediaTagType;

public class MediaTagImpl implements MediaTag {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long dbRowId;
	private String tag;
	private MediaTagType type;
	private MediaTagClassification classification;
	
	public MediaTagImpl (long dbRowId, String tag, MediaTagType type) {
		this.dbRowId = dbRowId;
		this.tag = tag;
		this.type = type;
		this.classification = null;
	}
	
	public MediaTagImpl (long dbRowId, String tag, MediaTagType type, MediaTagClassification classification) {
		this.dbRowId = dbRowId;
		this.tag = tag;
		this.type = type;
		this.classification = classification;
	}
	
	@Override
	public long getDbRowId() {
		return this.dbRowId;
	}
	
	@Override
	public boolean setDbRowId(long dbRowId) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getTag() {
		return this.tag;
	}
	@Override
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	@Override
	public MediaTagType getType() {
		return this.type;
	}
	@Override
	public void setType(MediaTagType type) {
		this.type = type;
	}
	
	@Override
	public MediaTagClassification getClassification() {
		return this.classification;
	}
	@Override
	public void setClassification(MediaTagClassification classification) {
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
