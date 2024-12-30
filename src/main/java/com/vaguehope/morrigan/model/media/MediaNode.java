package com.vaguehope.morrigan.model.media;

import java.util.AbstractList;
import java.util.List;

public class MediaNode extends AbstractList<AbstractItem> implements AbstractItem {

	private final String id;
	private final String title;
	private final String parentId;
	private final List<MediaNode> nodes;
	private final List<IMediaItem> items;

	public MediaNode(final String id, final String title, final String parentId, final List<MediaNode> nodes, final List<IMediaItem> items) {
		this.id = id;
		this.title = title;
		this.parentId = parentId;
		this.nodes = nodes;
		this.items = items;
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

	public List<MediaNode> getNodes() {
		return this.nodes;
	}

	public List<IMediaItem> getItems() {
		return this.items;
	}

	@Override
	public int size() {
		return this.nodes.size() + this.items.size();
	}

	@Override
	public AbstractItem get(int index) {
		if (index < this.nodes.size()) {
			return this.nodes.get(index);
		}
		index -= this.nodes.size();
		return this.items.get(index);
	}

	public int indexOf(final AbstractItem item) {
		int i = this.nodes.indexOf(item);
		if (i >= 0) return i;
		return this.items.indexOf(item);
	}

}
