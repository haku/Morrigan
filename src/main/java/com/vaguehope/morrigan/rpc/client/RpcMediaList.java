package com.vaguehope.morrigan.rpc.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.MediaItem;
import com.vaguehope.morrigan.dlna.extcd.EphemeralMediaList;
import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.MediaNode;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class RpcMediaList extends EphemeralMediaList {

	private static final String ROOT_NODE_ID = "0";

	private final RemoteInstance ri;
	private final RpcClient rpcClient;
	private final Function<String, String> itemRemoteLocation;
	private final MetadataStorage metadataStorage;

	public RpcMediaList(final RemoteInstance ri, final RpcClient rpcClient, final Function<String, String> itemRemoteLocation, final IMediaItemStorageLayer storage) {
		this.ri = ri;
		this.rpcClient = rpcClient;
		this.itemRemoteLocation = itemRemoteLocation;
		this.metadataStorage = new MetadataStorage(storage);
	}

	@Override
	public String getListId() {
		return this.ri.getLocalIdentifier();
	}

	@Override
	public String getListName() {
		return this.ri.getLocalIdentifier();
	}

	@Override
	public UUID getUuid() {
		return UUID.nameUUIDFromBytes(this.ri.getLocalIdentifier().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public MediaListType getType() {
		return MediaListType.EXTMMDB;
	}

	private MediaBlockingStub blockingStub() {
		return this.rpcClient.getMediaBlockingStub(this.ri.getLocalIdentifier());
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

	@Override
	public FileExistance hasFile(final String identifer) throws MorriganException, DbException {
		final HasMediaReply ret = blockingStub().hasMedia(HasMediaRequest.newBuilder().setId(identifer).build());
		return convertExistance(ret.getExistance());
	}

	@Override
	public IMediaItem getByFile(final String identifer) throws DbException {
		final HasMediaReply ret = blockingStub().hasMedia(HasMediaRequest.newBuilder().setId(identifer).build());
		if (ret.getExistance() != com.vaguehope.dlnatoad.rpc.MediaToadProto.FileExistance.EXISTS) return null;
		return makeItem(ret.getItem());
	}

	private RpcMediaItem makeItem(final MediaItem item) throws DbException {
		return new RpcMediaItem(item, this.itemRemoteLocation, this.metadataStorage.getMetadataProxy(item.getId()));
	}

	@Override
	public List<IMediaItem> search(
			final MediaType mediaType,
			final String term,
			final int maxResults,
			final IDbColumn[] sortColumns,
			final SortDirection[] sortDirections,
			final boolean includeDisabled) throws DbException {
		// TODO Auto-generated method stub
		return null;
	}

	private static FileExistance convertExistance(final com.vaguehope.dlnatoad.rpc.MediaToadProto.FileExistance existance) {
		switch (existance) {
		case EXISTS:
			return FileExistance.EXISTS;
		case MISSING:
			return FileExistance.MISSING;
		case UNKNOWN:
		case UNRECOGNIZED:
		default:
			return FileExistance.UNKNOWN;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Metadata.

	@Override
	public void incTrackStartCnt(final IMediaItem item) throws MorriganException {
		this.metadataStorage.incTrackStartCnt(item);
	}

	@Override
	public void incTrackEndCnt(final IMediaItem item) throws MorriganException {
		this.metadataStorage.incTrackEndCnt(item);
	}

}
