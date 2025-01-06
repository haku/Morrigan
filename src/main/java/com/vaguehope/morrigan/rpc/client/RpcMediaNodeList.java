package com.vaguehope.morrigan.rpc.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.MediaItem;
import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.AbstractItem;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.MediaNode;

public class RpcMediaNodeList extends RpcMediaList {

	private final String nodeId;
	private final String title;

	private volatile List<MediaNode> mediaNodes = Collections.emptyList();
	private volatile List<IMediaItem> mediaItems = Collections.emptyList();
	private volatile long durationOfLastRead = -1;

	public RpcMediaNodeList(final String nodeId, final String title, final RemoteInstance ri, final RpcClient rpcClient, final RpcContentServlet rpcContentServer, final MetadataStorage storage) {
		super(ri, rpcClient, rpcContentServer, storage);
		this.nodeId = nodeId;
		this.title = title;
	}

	@Override
	public String getSerial() {
		return new RpcListSerial(this.ri.getLocalIdentifier(), null).serialise();
	}

	@Override
	public boolean hasNodes() {
		return true;
	}

	@Override
	public void forceRead() throws MorriganException {
		final long start = System.nanoTime();
		final ListNodeReply resp = blockingStub().listNode(ListNodeRequest.newBuilder().setNodeId(this.nodeId).build());

		final List<MediaNode> nodes = new ArrayList<>(resp.getChildCount());
		for (final MediaToadProto.MediaNode n : resp.getChildList()) {
			nodes.add(new MediaNode(n.getId(), n.getTitle(), this.nodeId));
		}

		final List<IMediaItem> items = new ArrayList<>(resp.getItemCount());
		for (final MediaItem i : resp.getItemList()) {
			items.add(makeItem(i));
		}

		this.mediaNodes = nodes;
		this.mediaItems = items;
		this.durationOfLastRead = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
	}

	@Override
	public long getDurationOfLastRead() {
		return this.durationOfLastRead;
	}

	@Override
	public String getListName() {
		if (StringUtils.isBlank(this.title)) return super.getListName();
		return super.getListName() + ": " + this.title;
	}

	@Override
	public int size() {
		return this.mediaNodes.size() + this.mediaItems.size();
	}

	@Override
	public AbstractItem get(int index) {
		if (index < this.mediaNodes.size()) {
			return this.mediaNodes.get(index);
		}
		index -= this.mediaNodes.size();
		return this.mediaItems.get(index);
	}

	@Override
	public List<MediaNode> getSubNodes() throws MorriganException {
		return this.mediaNodes;
	}

	@Override
	public List<IMediaItem> getMediaItems() {
		return this.mediaItems;
	}

	@Override
	public int indexOf(Object o) {
		int i = this.mediaNodes.indexOf(o);
		if (i >= 0) return i;
		return this.mediaItems.indexOf(o);
	}

}
