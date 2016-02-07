package com.vaguehope.morrigan.model.media;

import java.util.Date;

import com.vaguehope.morrigan.model.db.IDbItem;


public interface MediaTag extends IDbItem {

	String getTag();
	MediaTagType getType();
	MediaTagClassification getClassification();

	Date getModified();
	boolean isDeleted();

}
