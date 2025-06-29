package com.vaguehope.morrigan.rpc.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ExcludedChange;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.RecordPlaybackRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SearchReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SearchRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SortBy;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SortField;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.TagAction;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.TagChange;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.UpdateExcludedRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.UpdateTagsRequest;
import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.EphemeralMediaList;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.SortColumn;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
import com.vaguehope.morrigan.player.contentproxy.ContentProxy;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

import io.grpc.StatusRuntimeException;

public abstract class RpcMediaList extends EphemeralMediaList {

	protected final ListRef listRef;
	protected final RemoteInstance ri;
	protected final RpcItemCache itemCache;
	private final RpcClient rpcClient;
	private final MetadataStorage metadataStorage;
	private final RpcContentServlet rpcContentServer;

	private volatile boolean neverRead = true;
	protected volatile List<String> mediaItemIds = Collections.emptyList();
	private volatile long durationOfLastRead = -1;

	public RpcMediaList(
			final ListRef listRef,
			final RemoteInstance ri,
			final RpcClient rpcClient,
			final RpcItemCache itemCache,
			final RpcContentServlet rpcContentServer,
			final MetadataStorage metadataStorage) {
		this.listRef = listRef;
		this.ri = ri;
		this.rpcClient = rpcClient;
		this.itemCache = itemCache;
		this.rpcContentServer = rpcContentServer;
		this.metadataStorage = metadataStorage;
	}

	@Override
	public ListRef getListRef() {
		return this.listRef;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof RpcMediaList)) return false;
		final RpcMediaList that = (RpcMediaList) obj;
		return Objects.equals(this.listRef, that.listRef);
	}

	@Override
	public int hashCode() {
		return this.listRef.hashCode();
	}

	@Override
	public String getListName() {
		return this.listRef.getListId();
	}

	@Override
	public String toString() {
		return String.format("%s{%s, %s}", getClass().getSimpleName(), this.listRef, getListName());
	}

	@Override
	public UUID getUuid() {
		return UUID.nameUUIDFromBytes(this.ri.getLocalIdentifier().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public MediaList makeNode(final String nodeId, final String title) throws MorriganException {
		final ListRef ref = ListRef.forRpcNode(this.listRef.getListId(), nodeId);
		return new RpcMediaNodeList(ref, title, this.ri, this.rpcClient, this.itemCache, this.rpcContentServer, this.metadataStorage);
	}

	@Override
	public boolean canMakeView() {
		return true;
	}

	@Override
	public MediaList makeView(final String filter) throws MorriganException {
		final ListRef ref = ListRef.forRpcSearch(this.listRef.getListId(), filter);
		return new RpcMediaSearchList(ref, this.ri, this.rpcClient, this.itemCache, this.rpcContentServer, this.metadataStorage);
	}

	protected MediaBlockingStub blockingStub() {
		return this.rpcClient.getMediaBlockingStub(this.ri.getLocalIdentifier());
	}

	@Override
	public void read() throws MorriganException {
		if (this.neverRead) forceRead();
	}

	@Override
	public void forceRead() throws MorriganException {
		this.neverRead = false;
	}

	protected void setMediaItems(final List<MediaItem> items, final long durationOfLastRead) {
		this.mediaItemIds = items.stream().map(i -> i.getRemoteId()).collect(Collectors.toList());
		this.durationOfLastRead = durationOfLastRead;
	}

	@Override
	public List<MediaItem> getMediaItems() {
		return this.itemCache.getForIds(this.mediaItemIds);
	}

	@Override
	public long getDurationOfLastRead() {
		return this.durationOfLastRead;
	}

	@Override
	public FileExistance hasFile(final String identifer) throws MorriganException {
		// TODO is it worth caching misses?
		if (this.itemCache.getIfFresh(identifer) != null) return FileExistance.EXISTS;
		try {
			final HasMediaReply ret = blockingStub().hasMedia(HasMediaRequest.newBuilder().setId(identifer).build());
			return convertExistance(ret.getExistence());
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("hasMedia() RPC failed: " + e.toString(), e);
		}
	}

	@Override
	public MediaItem getByFile(final String identifer) throws MorriganException {
		final MediaItem freshCache = this.itemCache.getIfFresh(identifer);
		if (freshCache != null) return freshCache;
		try {
			final HasMediaReply ret = blockingStub().hasMedia(HasMediaRequest.newBuilder().setId(identifer).build());
			if (ret.getExistence() != MediaToadProto.FileExistance.EXISTS) return null;
			return makeItem(ret.getItem());
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("hasMedia() RPC failed: " + e.toString(), e);
		}
	}

	protected RpcMediaItem makeItem(final MediaToadProto.MediaItem item) throws DbException {
		final RpcMediaItem mi = new RpcMediaItem(item, this.metadataStorage.getMetadataProxy(item.getId()));
		this.itemCache.put(mi);
		return mi;
	}

	@Override
	public String prepairRemoteLocation(final MediaItem item, final ContentProxy contentProxy) {
		return contentProxy.makeUri(this.rpcContentServer, this.ri.getLocalIdentifier(), item.getRemoteId());
	}

	@Override
	public List<MediaItem> search(
			final MediaType mediaType,
			final String term,
			final int maxResults,
			final SortColumn[] sortColumns,
			final SortDirection[] sortDirections,
			final boolean includeDisabled) throws MorriganException {
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
				case UNSPECIFIED:
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.UNSPECIFIED_ORDER).setDirection(dir).build());
					break;
				case FILE_PATH:
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.FILE_PATH).setDirection(dir).build());
					break;
				case DATE_ADDED:
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.DATE_ADDED).setDirection(dir).build());
					break;
				case DURATION:
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.DURATION).setDirection(dir).build());
					break;
				case DATE_LAST_PLAYED:
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.LAST_PLAYED).setDirection(dir).build());
					break;
				case START_COUNT:
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.PLAYBACK_STARTED).setDirection(dir).build());
					break;
				case END_COUNT:
					req.addSortBy(SortBy.newBuilder().setSortField(SortField.PLAYBACK_COMPLETED).setDirection(dir).build());
					break;
				default:
					throw new DbException("Unsupported sort column: " + sortColumns[i]);
				}
			}
		}

		final SearchReply resp;
		try {
			resp = blockingStub().search(req.build());
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("search() RPC failed: " + e.toString(), e);
		}

		final List<MediaItem> items = new ArrayList<>(resp.getResultList().size());
		for (final MediaToadProto.MediaItem i : resp.getResultList()) {
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

	@Override
	public void addTag(final MediaItem item, final String tag) throws MorriganException {
		final MediaItem latestItem = this.itemCache.getForId(item.getId());
		if (latestItem == null) throw new IllegalArgumentException("Not in cache: " + item.getId());

		try {
			final MediaToadProto.MediaTag protoTag = MediaToadProto.MediaTag.newBuilder()
					.setTag(tag)
					.build();
			blockingStub().updateTags(UpdateTagsRequest.newBuilder()
					.addChange(TagChange.newBuilder()
							.setId(item.getRemoteId())
							.setAction(TagAction.ADD)
							.addTag(protoTag)
							.build())
					.build());

			this.itemCache.put(((RpcMediaItem) latestItem).withTag(new RpcTag(protoTag)));
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("updateTags() RPC failed: " + e.toString(), e);
		}
	}

	@Override
	public void removeTag (final MediaItem item, final MediaTag tag) throws MorriganException {
		final MediaItem latestItem = this.itemCache.getForId(item.getId());
		if (latestItem == null) throw new IllegalArgumentException("Not in cache: " + item.getId());

		try {
			final MediaToadProto.MediaTag protoTag = ((RpcTag) tag).getProtoTag();
			blockingStub().updateTags(UpdateTagsRequest.newBuilder()
					.addChange(TagChange.newBuilder()
							.setId(item.getRemoteId())
							.setAction(TagAction.REMOVE)
							.addTag(protoTag)
							.build())
					.build());

			this.itemCache.put(((RpcMediaItem) latestItem).withoutTag(tag));
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("updateTags() RPC failed: " + e.toString(), e);
		}
	}

	@Override
	public List<MediaTag> getTagsIncludingDeleted(final MediaItem item) throws MorriganException {
		return item.getTags();
	}

	@Override
	public void setItemEnabled(final MediaItem item, final boolean value) throws MorriganException {
		final MediaItem latestItem = this.itemCache.getForId(item.getId());
		if (latestItem == null) throw new IllegalArgumentException("Not in cache: " + item.getId());

		try {
			final boolean exluded = !value;
			blockingStub().updateExcluded(UpdateExcludedRequest.newBuilder()
					.addChange(ExcludedChange.newBuilder()
							.setId(item.getRemoteId())
							.setExcluded(exluded)
							.build())
					.build());

			this.itemCache.put(((RpcMediaItem) latestItem).withEnabled(exluded));
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("updateExcluded() RPC failed: " + e.toString(), e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Metadata.

	@Override
	public void incTrackStartCnt(final MediaItem item) throws MorriganException {
		this.metadataStorage.incTrackStartCnt(item);
	}

	@Override
	public void incTrackEndCnt(final MediaItem item, final boolean completed, final long startTime) throws MorriganException {
		this.metadataStorage.incTrackEndCnt(item);

		try {
			blockingStub().recordPlayback(RecordPlaybackRequest.newBuilder()
					.setId(item.getRemoteId())
					.setCompleted(completed)
					.setStartTimeMillis(startTime)
					.build());
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("recordPlayback() RPC failed: " + e.toString(), e);
		}
	}

}
