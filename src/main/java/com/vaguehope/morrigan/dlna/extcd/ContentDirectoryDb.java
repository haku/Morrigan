package com.vaguehope.morrigan.dlna.extcd;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.support.contentdirectory.callback.Browse;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.container.Container;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.AbstractItem;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaNode;
import com.vaguehope.morrigan.model.media.SortColumn;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class ContentDirectoryDb extends EphemeralMediaList {

	private static final int MAX_TRIES = 2;
	private static final int MAX_ITEMS = 500;

	private final ListRef listRef;
	private final RemoteDevice device;
	private final ControlPoint controlPoint;
	private final RemoteService remoteService;
	private final ContentDirectory contentDirectory;
	private final MetadataStorage storage;

	private volatile String title;
	private volatile List<MediaNode> mediaNodes = Collections.emptyList();
	private volatile List<MediaItem> mediaItems = Collections.emptyList();
	private volatile long durationOfLastRead = -1;

	public ContentDirectoryDb (
			final ListRef listRef,
			final String title,
			final ControlPoint controlPoint,
			final RemoteDevice device,
			final RemoteService remoteService,
			final MetadataStorage storage,
			final ContentDirectory contentDirectory) {
		this.listRef = listRef;
		this.controlPoint = controlPoint;
		this.device = device;
		this.remoteService = remoteService;
		this.storage = storage;
		this.contentDirectory = contentDirectory;
		this.title = title;
	}

	@Override
	public void dispose () {
		super.dispose();
		this.storage.dispose();
	}

	@Override
	public ListRef getListRef() {
		return this.listRef;
	}

	@Override
	public UUID getUuid () {
		return UUID.fromString(this.listRef.getListId());
	}

	@Override
	public String getListName () {
		String name = this.device.getDetails().getFriendlyName();
		if (StringUtils.isNotBlank(this.title)) name += ": " + this.title;
		return name;
	}

	@Override
	public FileExistance hasFile (final String remoteId) throws MorriganException, DbException {
		return this.contentDirectory.fetchItemByIdWithRetry(remoteId, MAX_TRIES) != null
				? FileExistance.EXISTS
				: FileExistance.UNKNOWN;
	}

	@Override
	public MediaItem getByFile (final String remoteId) throws DbException {
		final MediaItem item = this.contentDirectory.fetchItemByIdWithRetry(remoteId, MAX_TRIES);
		if (item == null) throw new IllegalArgumentException("File with ID '" + remoteId + "' not found.");
		return item;
	}

	@Override
	public List<MediaItem> search(final MediaType mediaType, final String term, final int maxResults, final SortColumn[] sortColumns, final SortDirection[] sortDirections, final boolean includeDisabled) throws DbException {
		return this.contentDirectory.searchWithRetry(term, maxResults, MAX_TRIES);
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
	public MediaList makeNode(final String nodeId, final String nodeTitle) throws MorriganException {
		final ListRef ref = ListRef.forDlnaNode(this.listRef.getListId(), nodeId);
		return new ContentDirectoryDb(ref, nodeTitle, this.controlPoint, this.device, this.remoteService, this.storage, this.contentDirectory);
	}

	@Override
	public void read() throws MorriganException {
		forceRead();  // TODO check if this needs optimising.
	}

	@Override
	public void forceRead() throws MorriganException {
		final long start = System.nanoTime();

		final CountDownLatch cdl = new CountDownLatch(2);
		final SyncBrowse mdReq = new SyncBrowse(cdl, this.remoteService, this.listRef.getNodeId(), BrowseFlag.METADATA, Browse.CAPS_WILDCARD, 0, 1L);
		final SyncBrowse dcReq = new SyncBrowse(cdl, this.remoteService, this.listRef.getNodeId(), BrowseFlag.DIRECT_CHILDREN, Browse.CAPS_WILDCARD, 0, (long) MAX_ITEMS);
		this.controlPoint.execute(mdReq);
		this.controlPoint.execute(dcReq);

		ContentDirectory.await(cdl, "Browse '%s' on content directory '%s'.", this.listRef.getNodeId(), this.contentDirectory);
		if (mdReq.getRef() == null) throw new MorriganException(mdReq.getErr());
		if (dcReq.getRef() == null) throw new MorriganException(dcReq.getErr());

		if (mdReq.getRef().getContainers().size() < 1) {
			throw new MorriganException("Container not found: " + this.listRef.getNodeId());
		}

		final Container cont = mdReq.getRef().getContainers().get(0);
		this.title = cont.getTitle();

		final List<MediaNode> nodes = ContentDirectory.didlContainersToNodes(dcReq.getRef().getContainers());
		final List<MediaItem> items = this.contentDirectory.didlItemsToMnItems(dcReq.getRef().getItems());

		this.mediaNodes = nodes;
		this.mediaItems = items;
		this.durationOfLastRead = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
	}

	@Override
	public long getDurationOfLastRead() {
		return this.durationOfLastRead;
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

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Metadata.

	@Override
	public void incTrackStartCnt (final MediaItem item) throws MorriganException {
		this.storage.incTrackStartCnt(item);
	}

	@Override
	public void incTrackEndCnt (final MediaItem item) throws MorriganException {
		this.storage.incTrackEndCnt(item);
	}

}
