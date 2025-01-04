package com.vaguehope.morrigan.rpc.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.MediaItem;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SearchReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SearchRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SortBy;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SortField;
import com.vaguehope.morrigan.dlna.extcd.EphemeralMediaList;
import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMixedMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.MediaNode;
import com.vaguehope.morrigan.player.contentproxy.ContentProxy;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class RpcMediaList extends EphemeralMediaList {

	private static final String ROOT_NODE_ID = "0";

	private final RemoteInstance ri;
	private final RpcClient rpcClient;
	private final MetadataStorage metadataStorage;
	private final RpcContentServlet rpcContentServer;

	public RpcMediaList(final RemoteInstance ri, final RpcClient rpcClient, final RpcContentServlet rpcContentServer, final IMediaItemStorageLayer storage) {
		this.ri = ri;
		this.rpcClient = rpcClient;
		this.rpcContentServer = rpcContentServer;
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
		return convertExistance(ret.getExistence());
	}

	@Override
	public IMediaItem getByFile(final String identifer) throws DbException {
		final HasMediaReply ret = blockingStub().hasMedia(HasMediaRequest.newBuilder().setId(identifer).build());
		if (ret.getExistence() != com.vaguehope.dlnatoad.rpc.MediaToadProto.FileExistance.EXISTS) return null;
		return makeItem(ret.getItem());
	}

	private RpcMediaItem makeItem(final MediaItem item) throws DbException {
		return new RpcMediaItem(item, this.metadataStorage.getMetadataProxy(item.getId()));
	}

	@Override
	public String prepairRemoteLocation(final IMediaItem item, final ContentProxy contentProxy) {
		return contentProxy.makeUri(this.rpcContentServer, this.ri.getLocalIdentifier(), item.getRemoteId());
	}

	@Override
	public List<IMediaItem> search(
			final MediaType mediaType,
			final String term,
			final int maxResults,
			final IDbColumn[] sortColumns,
			final SortDirection[] sortDirections,
			final boolean includeDisabled) throws DbException {
		if (sortColumns == null ^ sortDirections == null) throw new IllegalArgumentException("Must specify both or neith of sort and direction.");
		if (sortColumns != null && sortDirections != null && sortColumns.length != sortDirections.length) throw new IllegalArgumentException("Sorts and directions must be same length.");

		String modTerm = "(type=audio OR type=video)";
		if (StringUtils.isNotBlank(term)) modTerm += String.format(" AND ( %s )", term);

		final SearchRequest.Builder req = SearchRequest.newBuilder()
				.setQuery(modTerm)
				.setMaxResults(maxResults);

		if (sortColumns != null) {
			for (int i = 0; i < sortColumns.length; i++) {
				final IDbColumn col = sortColumns[i];
				final MediaToadProto.SortDirection dir = direction(sortDirections[i]);

				if (IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE.getName().equals(col.getName())) {
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.FILE_PATH).setDirection(dir).build());
				}
				else if (IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_DADDED.getName().equals(col.getName())) {
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.DATE_ADDED).setDirection(dir).build());
				}
				else if (IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_DURATION.getName().equals(col.getName())) {
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.DURATION).setDirection(dir).build());
				}
				else {
					throw new DbException("Unsupported sort column: " + col.getName());
				}
			}
		}

		final SearchReply resp = blockingStub().search(req.build());

		final List<IMediaItem> items = new ArrayList<>(resp.getResultList().size());
		for (final MediaItem i : resp.getResultList()) {
			items.add(makeItem(i));
		}
		return items;
	}

	private static MediaToadProto.SortDirection direction(final SortDirection dir) {
		switch (dir) {
		case ASC:
			return MediaToadProto.SortDirection.ASC;
		case DESC:
			return MediaToadProto.SortDirection.DESC;
		default:
			throw new IllegalArgumentException();
		}
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
