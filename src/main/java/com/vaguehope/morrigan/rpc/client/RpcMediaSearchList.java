package com.vaguehope.morrigan.rpc.client;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.SortColumn;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;

public class RpcMediaSearchList extends RpcMediaList {

	private static final int MAX_VIEW_SIZE = 10000;  // this is just a guess.

	private final String searchTerm;

	private volatile List<IMediaItem> mediaItems = Collections.emptyList();
	private volatile long durationOfLastRead = -1;
	private volatile SortColumn sortColumn = SortColumn.FILE_PATH;
	private volatile SortDirection sortDirection = SortDirection.ASC;

	public RpcMediaSearchList(
			final String searchTerm,
			final RemoteInstance ri,
			final RpcClient rpcClient,
			final RpcContentServlet rpcContentServer,
			final MetadataStorage metadataStorage) {
		super(ri, rpcClient, rpcContentServer, metadataStorage);
		this.searchTerm = searchTerm;
	}

	@Override
	public String getSerial() {
		return new RpcListSerial(this.ri.getLocalIdentifier(), this.searchTerm).serialise();
	}

	@Override
	public String getSearchTerm() {
		return this.searchTerm;
	}

	@Override
	public String getListName() {
		return super.getListName() + "{" + this.searchTerm + "}";
	}

	@Override
	public int getCount() {
		return this.mediaItems.size();
	}

	@Override
	public List<IMediaItem> getMediaItems() {
		return this.mediaItems;
	}

	@Override
	public boolean canSort() {
		return true;
	}
	@Override
	public void setSort(final SortColumn column, final SortDirection direction) throws MorriganException {
		this.sortColumn = column;
		this.sortDirection = direction;
	}
	@Override
	public SortColumn getSortColumn() {
		return this.sortColumn;
	}
	@Override
	public SortDirection getSortDirection() {
		return this.sortDirection;
	}

	@Override
	public void read() throws MorriganException {
		forceRead();  // TODO check if this needs optimising.
	}

	@Override
	public void forceRead() throws MorriganException {
		final long start = System.nanoTime();
		this.mediaItems = search(MediaType.TRACK, this.searchTerm, MAX_VIEW_SIZE,
				new SortColumn[] { this.sortColumn },
				new SortDirection[] { this.sortDirection },
				false);
		this.durationOfLastRead = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
	}

	@Override
	public long getDurationOfLastRead() {
		return this.durationOfLastRead;
	}

}
