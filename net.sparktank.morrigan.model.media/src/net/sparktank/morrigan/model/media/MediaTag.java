package net.sparktank.morrigan.model.media;


public interface MediaTag { // TODO extends IDbItem
	
	public long getDbRowId();
	
	public String getTag();
	public void setTag(String tag);
	
	public MediaTagType getType();
	public void setType(MediaTagType type);
	
	public MediaTagClassification getClassification();
	public void setClassification(MediaTagClassification classification);
	
}
