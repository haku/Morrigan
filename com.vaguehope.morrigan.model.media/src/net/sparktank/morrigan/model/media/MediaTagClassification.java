package net.sparktank.morrigan.model.media;

import net.sparktank.morrigan.model.db.IDbItem;

public interface MediaTagClassification extends IDbItem {
	
	public String getClassification();
	public void setClassification(String classification);
	
}
