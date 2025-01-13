package com.vaguehope.morrigan.rpc.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeRequest;
import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.AbstractItem;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaNode;
import com.vaguehope.morrigan.player.PlaybackOrder;

import io.grpc.StatusRuntimeException;

public class RpcMediaNodeList extends RpcMediaList {

	private volatile String title;
	private volatile List<MediaNode> mediaNodes = Collections.emptyList();
	private volatile List<MediaItem> mediaItems = Collections.emptyList();
	private volatile long durationOfLastRead = -1;

	public RpcMediaNodeList(final ListRef listRef, final String title, final RemoteInstance ri, final RpcClient rpcClient, final RpcContentServlet rpcContentServer, final MetadataStorage storage) {
		super(listRef, ri, rpcClient, rpcContentServer, storage);
		this.title = title;
	}

	@Override
	public boolean hasNodes() {
		return true;
	}

	@Override
	public String getNodeId() {
		return this.listRef.getNodeId();
	}

	@Override
	public void forceRead() throws MorriganException {
		final long start = System.nanoTime();
		final ListNodeReply resp;
		try {
			resp = blockingStub().listNode(ListNodeRequest.newBuilder().setNodeId(this.listRef.getNodeId()).build());
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("listNode() RPC failed: " + e.toString(), e);
		}

		if (StringUtils.isNotBlank(resp.getNode().getTitle())) this.title = resp.getNode().getTitle();

		final List<MediaNode> nodes = new ArrayList<>(resp.getChildCount());
		for (final MediaToadProto.MediaNode n : resp.getChildList()) {
			nodes.add(new MediaNode(n.getId(), n.getTitle(), this.listRef.getNodeId()));
		}

		final List<MediaItem> items = new ArrayList<>(resp.getItemCount());
		for (final MediaToadProto.MediaItem i : resp.getItemList()) {
			items.add(makeItem(i, Collections.emptyList()));
		}

		this.mediaNodes = nodes;
		this.mediaItems = items;
		this.durationOfLastRead = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
		super.forceRead();
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
	public List<MediaItem> getMediaItems() {
		return this.mediaItems;
	}

	@Override
	public int indexOf(final Object o) {
		final int i = this.mediaNodes.indexOf(o);
		if (i >= 0) return i;
		return this.mediaItems.indexOf(o);
	}

	@Override
	public List<PlaybackOrder> getSupportedChooseMethods() {
		return Arrays.asList(PlaybackOrder.SEQUENTIAL);
	}

	@Override
	public PlaybackOrder getDefaultChooseMethod() {
		return PlaybackOrder.SEQUENTIAL;
	}

	@Override
	public MediaItem chooseItem(final PlaybackOrder order, final MediaItem previousItem) throws MorriganException {
		switch (order) {
		case SEQUENTIAL:
			return chooseSequential(previousItem);
		default:
			throw new IllegalArgumentException("Unsupported choose method.");
		}
	}

	private MediaItem chooseSequential(final MediaItem previousItem) throws MorriganException {
		read();
		if (this.mediaItems.size() < 1) return null;
		if (previousItem == null) return this.mediaItems.get(0);

		int prevIndex = -1;
		for (int i = 0; i < this.mediaItems.size(); i++) {
			if (this.mediaItems.get(i).getRemoteId().equals(previousItem.getRemoteId())) {
				prevIndex = i;
				break;
			}
		}

		if (prevIndex < 0) return null;
		final int i = prevIndex < this.mediaItems.size() - 1 ? prevIndex + 1 : 0;
		return this.mediaItems.get(i);
	}

}
