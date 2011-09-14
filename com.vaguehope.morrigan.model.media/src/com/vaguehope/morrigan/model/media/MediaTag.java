package com.vaguehope.morrigan.model.media;

import com.vaguehope.morrigan.model.db.IDbItem;


public interface MediaTag extends IDbItem {
	
	public String getTag();
	public void setTag(String tag);
	
	public MediaTagType getType();
	public void setType(MediaTagType type);
	
	public MediaTagClassification getClassification();
	public void setClassification(MediaTagClassification classification);
	
}
