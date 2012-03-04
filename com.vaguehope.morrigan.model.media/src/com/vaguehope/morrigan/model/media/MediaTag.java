package com.vaguehope.morrigan.model.media;

import com.vaguehope.morrigan.model.db.IDbItem;


public interface MediaTag extends IDbItem {

	String getTag();
	void setTag(String tag);

	MediaTagType getType();
	void setType(MediaTagType type);

	MediaTagClassification getClassification();
	void setClassification(MediaTagClassification classification);

}
