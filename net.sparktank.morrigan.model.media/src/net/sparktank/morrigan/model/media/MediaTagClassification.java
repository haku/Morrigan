package net.sparktank.morrigan.model.media;

public interface MediaTagClassification { // TODO extends IDbItem
	
	public long getRowId();
	
	public String getClassification();
	public void setClassification(String classification);
	
}
