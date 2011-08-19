package net.sparktank.morrigan.model.media;

import com.vaguehope.morrigan.model.db.IDbItem;

public interface MediaTagClassification extends IDbItem {
	
	public String getClassification();
	public void setClassification(String classification);
	
}
