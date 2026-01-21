package morrigan.rpc.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import io.grpc.StatusRuntimeException;
import mediatoad.rpc.MediaToadProto;
import mediatoad.rpc.MediaToadProto.ListNodeReply;
import mediatoad.rpc.MediaToadProto.ListNodeRequest;
import morrigan.dlna.extcd.MetadataStorage;
import morrigan.model.exceptions.MorriganException;
import morrigan.model.media.AbstractItem;
import morrigan.model.media.ListRef;
import morrigan.model.media.MediaItem;
import morrigan.model.media.MediaNode;
import morrigan.player.PlaybackOrder;

public class RpcMediaNodeList extends RpcMediaList {

	private volatile String title;
	private volatile List<MediaNode> mediaNodes = Collections.emptyList();

	public RpcMediaNodeList(
			final ListRef listRef,
			final String title,
			final RemoteInstance ri,
			final RpcClient rpcClient,
			final RpcItemCache itemCache,
			final RpcContentServlet rpcContentServer,
			final MetadataStorage storage) {
		super(listRef, ri, rpcClient, itemCache, rpcContentServer, storage);
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
			items.add(makeItem(i));
		}

		this.mediaNodes = nodes;
		setMediaItems(items, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
		super.forceRead();
	}

	@Override
	public String getListName() {
		if (StringUtils.isBlank(this.title)) return super.getListName();
		return this.title;
	}

	@Override
	public String getLongListName() {
		if (StringUtils.isBlank(this.title)) return this.listRef.getListId();
		return this.listRef.getListId() + ": " + this.title;
	}

	@Override
	public int size() {
		return this.mediaNodes.size() + this.mediaItemIds.size();
	}

	@Override
	public AbstractItem get(int index) {
		if (index < this.mediaNodes.size()) {
			return this.mediaNodes.get(index);
		}
		index -= this.mediaNodes.size();
		return this.itemCache.getForId(this.mediaItemIds.get(index));
	}

	@Override
	public List<MediaNode> getSubNodes() throws MorriganException {
		return this.mediaNodes;
	}

	@Override
	public int indexOf(final Object o) {
		final int i = this.mediaNodes.indexOf(o);
		if (i >= 0) return i;

		if (!(o instanceof MediaItem)) return -1;
		final MediaItem mi = (MediaItem) o;
		return this.mediaItemIds.indexOf(mi.getRemoteId());
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
		if (this.mediaItemIds.size() < 1) return null;
		if (previousItem == null) return this.itemCache.getForId(this.mediaItemIds.get(0));

		int prevIndex = -1;
		for (int i = 0; i < this.mediaItemIds.size(); i++) {
			if (this.mediaItemIds.get(i).equals(previousItem.getRemoteId())) {
				prevIndex = i;
				break;
			}
		}

		if (prevIndex < 0) return null;
		final int i = prevIndex < this.mediaItemIds.size() - 1 ? prevIndex + 1 : 0;
		return this.itemCache.getForId(this.mediaItemIds.get(i));
	}

}
