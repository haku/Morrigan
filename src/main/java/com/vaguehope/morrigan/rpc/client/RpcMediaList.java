package com.vaguehope.morrigan.rpc.client;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaRequest;
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
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class RpcMediaList extends EphemeralMediaList {

	private final MetadataStorage metadataStorage;
	private final RemoteInstance ri;
	private final RpcClient rpcClient;

	public RpcMediaList(final RemoteInstance ri, final RpcClient rpcClient, final IMediaItemStorageLayer storage) {
		this.ri = ri;
		this.rpcClient = rpcClient;
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
	public FileExistance hasFile(final String identifer) throws MorriganException, DbException {
		final MediaBlockingStub stub = blockingStub();
		final HasMediaReply ret = stub.hasMedia(HasMediaRequest.newBuilder().setId(identifer).build());
		return convertExistance(ret.getExistance());
	}

	@Override
	public IMediaItem getByFile(final String identifer) throws DbException {
		final MediaBlockingStub stub = blockingStub();
		final HasMediaReply ret = stub.hasMedia(HasMediaRequest.newBuilder().setId(identifer).build());

		if (ret.getExistance() != com.vaguehope.dlnatoad.rpc.MediaToadProto.FileExistance.EXISTS) return null;

		return new RpcMediaItem(identifer, ret, this.metadataStorage.getMetadataProxy(identifer));
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

	private static FileExistance convertExistance(com.vaguehope.dlnatoad.rpc.MediaToadProto.FileExistance existance) {
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
