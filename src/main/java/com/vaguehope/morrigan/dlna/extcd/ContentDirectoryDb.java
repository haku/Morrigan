package com.vaguehope.morrigan.dlna.extcd;

import java.util.List;
import java.util.UUID;

import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class ContentDirectoryDb extends EphemeralMediaList {

	private static final int MAX_TRIES = 2;

	private final String listId;
	private final RemoteDevice device;
	private final ContentDirectory contentDirectory;
	private final MetadataStorage metadataStorage;

	public ContentDirectoryDb (final String listId, final ControlPoint controlPoint,
			final RemoteDevice device, final RemoteService contentDirectory,
			final IMediaItemStorageLayer storage) {
		this.listId = listId;
		this.device = device;
		this.metadataStorage = new MetadataStorage(storage);
		this.contentDirectory = new ContentDirectory(controlPoint, contentDirectory, this.metadataStorage);
	}

	@Override
	public void dispose () {
		super.dispose();
		this.metadataStorage.dispose();
	}

	@Override
	public String getListId () {
		return this.listId;
	}

	@Override
	public UUID getUuid () {
		return UUID.fromString(this.listId);
	}

	@Override
	public String getListName () {
		return this.device.getDetails().getFriendlyName();
	}

	@Override
	public MediaListType getType () {
		return MediaListType.EXTMMDB;
	}

	@Override
	public FileExistance hasFile (final String remoteId) throws MorriganException, DbException {
		return this.contentDirectory.fetchItemByIdWithRetry(remoteId, MAX_TRIES) != null
				? FileExistance.EXISTS
				: FileExistance.UNKNOWN;
	}

	@Override
	public IMediaItem getByFile (final String remoteId) throws DbException {
		final IMediaItem item = this.contentDirectory.fetchItemByIdWithRetry(remoteId, MAX_TRIES);
		if (item == null) throw new IllegalArgumentException("File with ID '" + remoteId + "' not found.");
		return item;
	}

	@Override
	public List<IMediaItem> search(final MediaType mediaType, final String term, final int maxResults, final IDbColumn[] sortColumns, final SortDirection[] sortDirections, final boolean includeDisabled) throws DbException {
		return this.contentDirectory.searchWithRetry(term, maxResults, MAX_TRIES);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Metadata.

	@Override
	public void incTrackStartCnt (final IMediaItem item) throws MorriganException {
		this.metadataStorage.incTrackStartCnt(item);
	}

	@Override
	public void incTrackEndCnt (final IMediaItem item) throws MorriganException {
		this.metadataStorage.incTrackEndCnt(item);
	}

}
