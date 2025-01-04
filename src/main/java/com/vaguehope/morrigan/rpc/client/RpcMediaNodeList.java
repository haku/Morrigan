package com.vaguehope.morrigan.rpc.client;

import java.util.ArrayList;
import java.util.List;

import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.MediaItem;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaNode;

public class RpcMediaNodeList extends RpcMediaList {

	public RpcMediaNodeList(final RemoteInstance ri, final RpcClient rpcClient, final RpcContentServlet rpcContentServer, final IMediaItemStorageLayer storage) {
		super(ri, rpcClient, rpcContentServer, storage);
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
	public MediaNode getRootNode() throws MorriganException {
		return getNode(ROOT_NODE_ID);
	}

	@Override
	public MediaNode getNode(final String id) throws MorriganException {
		final ListNodeReply resp = blockingStub().listNode(ListNodeRequest.newBuilder().setNodeId(id).build());

		final List<MediaNode> nodes = new ArrayList<>(resp.getChildCount());
		for (final MediaToadProto.MediaNode n : resp.getChildList()) {
			nodes.add(new MediaNode(n.getId(), n.getTitle(), id, null, null));
		}

		final List<IMediaItem> items = new ArrayList<>(resp.getItemCount());
		for (final MediaItem i : resp.getItemList()) {
			items.add(makeItem(i));
		}

		return new MediaNode(id, resp.getNode().getTitle(), resp.getNode().getParentId(), nodes, items);
	}

}
