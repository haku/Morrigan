package net.sparktank.morrigan.android.model.impl;

import net.sparktank.morrigan.android.model.MlistItem;

public class MlistItemBasicImpl implements MlistItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String title;
	private int type;
	private String relativeUrl;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		return this.title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	public int getType() {
		return this.type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public void setRelativeUrl(String url) {
		this.relativeUrl = url;
	}
	
	@Override
	public String getRelativeUrl() {
		return this.relativeUrl;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
