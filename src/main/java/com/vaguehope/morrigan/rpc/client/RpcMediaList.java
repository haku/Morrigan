package com.vaguehope.morrigan.rpc.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.MediaItem;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.MediaTag;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SearchReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SearchRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SortBy;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SortField;
import com.vaguehope.morrigan.dlna.extcd.EphemeralMediaList;
import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.SortColumn;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
import com.vaguehope.morrigan.player.contentproxy.ContentProxy;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public abstract class RpcMediaList extends EphemeralMediaList {

	protected final RemoteInstance ri;
	private final RpcClient rpcClient;
	private final MetadataStorage metadataStorage;
	private final RpcContentServlet rpcContentServer;

	public RpcMediaList(final RemoteInstance ri, final RpcClient rpcClient, final RpcContentServlet rpcContentServer, final MetadataStorage metadataStorage) {
		this.ri = ri;
		this.rpcClient = rpcClient;
		this.rpcContentServer = rpcContentServer;
		this.metadataStorage = metadataStorage;
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
	public String toString() {
		return String.format("%s{%s, %s}", getClass().getSimpleName(), getListId(), getListName());
	}

	@Override
	public UUID getUuid() {
		return UUID.nameUUIDFromBytes(this.ri.getLocalIdentifier().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public MediaListType getType() {
		return MediaListType.EXTMMDB;
	}

	@Override
	public IMediaItemList makeNode(final String id, final String title) throws MorriganException {
		return new RpcMediaNodeList(id, title, this.ri, this.rpcClient, this.rpcContentServer, this.metadataStorage);
	}

	@Override
	public boolean canMakeView() {
		return true;
	}

	@Override
	public IMediaItemList makeView(final String filter) throws MorriganException {
		return new RpcMediaSearchList(filter, this.ri, this.rpcClient, this.rpcContentServer, this.metadataStorage);
	}

	protected MediaBlockingStub blockingStub() {
		return this.rpcClient.getMediaBlockingStub(this.ri.getLocalIdentifier());
	}

	@Override
	public void read() throws MorriganException {
		forceRead();  // TODO check if this needs optimising.
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
		return makeItem(ret.getItem(), ret.getTagList());
	}

	protected RpcMediaItem makeItem(final MediaItem item, final List<MediaTag> tags) throws DbException {
		return new RpcMediaItem(item, tags, this.metadataStorage.getMetadataProxy(item.getId()));
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
			final SortColumn[] sortColumns,
			final SortDirection[] sortDirections,
			final boolean includeDisabled) throws DbException {
		if (sortColumns == null ^ sortDirections == null) throw new IllegalArgumentException("Must specify both or neith of sort and direction.");
		if (sortColumns != null && sortDirections != null && sortColumns.length != sortDirections.length) throw new IllegalArgumentException("Sorts and directions must be same length.");

		// TODO do this based on mediaType ?
		String modTerm = "(type=audio OR type=video)";
		if (StringUtils.isNotBlank(term)) modTerm += String.format(" AND ( %s )", term);

		final SearchRequest.Builder req = SearchRequest.newBuilder()
				.setQuery(modTerm)
				.setMaxResults(maxResults);

		if (sortColumns != null) {
			for (int i = 0; i < sortColumns.length; i++) {
				final MediaToadProto.SortDirection dir = direction(sortDirections[i]);
				switch (sortColumns[i]) {
				case FILE_PATH:
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.FILE_PATH).setDirection(dir).build());
					break;
				case DATE_ADDED:
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.DATE_ADDED).setDirection(dir).build());
					break;
				case DURATION:
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.DURATION).setDirection(dir).build());
					break;
				default:
					throw new DbException("Unsupported sort column: " + sortColumns[i]);
				}
			}
		}

		final SearchReply resp = blockingStub().search(req.build());

		final List<IMediaItem> items = new ArrayList<>(resp.getResultList().size());
		for (final MediaItem i : resp.getResultList()) {
			items.add(makeItem(i, Collections.emptyList()));
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
