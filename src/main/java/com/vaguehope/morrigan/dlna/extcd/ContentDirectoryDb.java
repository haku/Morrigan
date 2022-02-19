package com.vaguehope.morrigan.dlna.extcd;

import java.util.List;
import java.util.UUID;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMixedMediaStorageLayer;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class ContentDirectoryDb extends EphemeralMixedMediaDb {

	private static final int MAX_TRIES = 2;

	private final String listId;
	private final RemoteDevice device;
	private final ContentDirectory contentDirectory;
	private final MetadataStorage metadataStorage;

	public ContentDirectoryDb (final String listId, final ControlPoint controlPoint,
			final RemoteDevice device, final RemoteService contentDirectory,
			final IMixedMediaStorageLayer storage) {
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
	public String getType () {
		return MediaListType.EXTMMDB.toString();
	}

	@Override
	public FileExistance hasFile (final String remoteId) throws MorriganException, DbException {
		return this.contentDirectory.fetchItemByIdWithRetry(remoteId, MAX_TRIES) != null
				? FileExistance.EXISTS
				: FileExistance.UNKNOWN;
	}

	@Override
	public IMixedMediaItem getByFile (final String remoteId) throws DbException {
		final IMixedMediaItem item = this.contentDirectory.fetchItemByIdWithRetry(remoteId, MAX_TRIES);
		if (item == null) throw new IllegalArgumentException("File with ID '" + remoteId + "' not found.");
		return item;
	}

	@Override
	public List<IMixedMediaItem> simpleSearchMedia (final MediaType mediaType, final String term, final int maxResults, final IDbColumn[] sortColumns, final SortDirection[] sortDirections, final boolean includeDisabled) throws DbException {
		return this.contentDirectory.searchWithRetry(term, maxResults, MAX_TRIES);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Metadata.

	@Override
	public void incTrackStartCnt (final IMediaTrack item) throws MorriganException {
		this.metadataStorage.incTrackStartCnt(item);
	}

	@Override
	public void incTrackEndCnt (final IMediaTrack item) throws MorriganException {
		this.metadataStorage.incTrackEndCnt(item);
	}

}
