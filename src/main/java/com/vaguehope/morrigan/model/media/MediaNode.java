package com.vaguehope.morrigan.model.media;

public class MediaNode implements AbstractItem {

	private final String id;
	private final String title;
	private final String parentId;

	public MediaNode(final String id, final String title, final String parentId) {
		this.id = id;
		this.title = title;
		this.parentId = parentId;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public String getTitle() {
		return this.title;
	}

	public String getParentId() {
		return this.parentId;
	}

}
