package com.vaguehope.morrigan.rpc.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.vaguehope.dlnatoad.rpc.MediaToadProto.AboutReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.AboutRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ChooseMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ChooseMediaRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ChooseMethod;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SortField;
import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.AbstractItem;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.model.media.SortColumn;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
import com.vaguehope.morrigan.player.PlaybackOrder;

import io.grpc.StatusRuntimeException;

public class RpcMediaSearchList extends RpcMediaList {

	private static final int MAX_VIEW_SIZE = 10000;  // this is just a guess.

	private volatile SortColumn sortColumn = null;
	private volatile SortDirection sortDirection = SortDirection.ASC;
	private volatile List<SortColumn> supportSortColumns = null;
	private volatile List<PlaybackOrder> supportChooseMethods = null;

	public RpcMediaSearchList(
			final ListRef listRef,
			final RemoteInstance ri,
			final RpcClient rpcClient,
			final RpcItemCache itemCache,
			final RpcContentServlet rpcContentServer,
			final MetadataStorage metadataStorage) {
		super(listRef, ri, rpcClient, itemCache, rpcContentServer, metadataStorage);
	}

	@Override
	public String getListName() {
		return super.getListName() + "{" + this.listRef.getSearch() + "}";
	}

	@Override
	public boolean canSort() {
		return true;
	}
	@Override
	public List<SortColumn> getSuportedSortColumns() throws MorriganException {
		if (this.supportSortColumns != null) return this.supportSortColumns;

		final AboutReply about;
		try {
			about = blockingStub().about(AboutRequest.newBuilder().build());
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("about() RPC failed: " + e.toString(), e);
		}

		final List<SortColumn> ret = new ArrayList<>();
		for (final SortField sf : about.getSupportedSortFieldList()) {
			switch (sf) {
			case FILE_PATH:
				ret.add(SortColumn.FILE_PATH);
				break;
			case DATE_ADDED:
				ret.add(SortColumn.DATE_ADDED);
				break;
			case DURATION:
				ret.add(SortColumn.DURATION);
				break;
			default:
			}
		}
		if (ret.size() < 1) ret.add(SortColumn.UNSPECIFIED);
		this.supportSortColumns = ret;
		return ret;
	}
	@Override
	public void setSort(final SortColumn column, final SortDirection direction) throws MorriganException {
		this.sortColumn = column;
		this.sortDirection = direction;
	}
	@Override
	public SortColumn getSortColumn() {
		if (this.sortColumn == null) {
			try {
				this.sortColumn = getSuportedSortColumns().get(0);
			}
			catch (final MorriganException e) {
				return SortColumn.UNSPECIFIED;
			}
		}

		return this.sortColumn;
	}
	@Override
	public SortDirection getSortDirection() {
		return this.sortDirection;
	}

	@Override
	public void forceRead() throws MorriganException {
		final long start = System.nanoTime();
		final List<MediaItem> items = search(MediaType.TRACK, this.listRef.getSearch(), MAX_VIEW_SIZE,
				new SortColumn[] { getSortColumn() },
				new SortDirection[] { this.sortDirection },
				false);
		setMediaItems(items, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
		super.forceRead();
	}

	@Override
	public int size() {
		return this.mediaItemIds.size();
	}

	@Override
	public AbstractItem get(final int index) {
		return this.itemCache.getForId(this.mediaItemIds.get(index));
	}

	@Override
	public int indexOf(final Object o) {
		if (!(o instanceof MediaItem)) return -1;
		final MediaItem mi = (MediaItem) o;
		return this.mediaItemIds.indexOf(mi.getRemoteId());
	}

	@Override
	public List<PlaybackOrder> getSupportedChooseMethods() throws MorriganException {
		if (this.supportChooseMethods != null) return this.supportChooseMethods;

		final AboutReply about;
		try {
			about = blockingStub().about(AboutRequest.newBuilder().build());
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("about() RPC failed: " + e.toString(), e);
		}

		final List<PlaybackOrder> ret = new ArrayList<>();
		for (final ChooseMethod cm : about.getSupportedChooseMethodList()) {
			switch (cm) {
			case RANDOM:
				ret.add(PlaybackOrder.RANDOM);
				break;
			case LESS_RECENT:
				ret.add(PlaybackOrder.BYLASTPLAYED);
				break;
			case LESS_PLAYED:
				ret.add(PlaybackOrder.BYSTARTCOUNT);
				break;
			default:
			}
		}
		if (ret.size() < 1) ret.add(PlaybackOrder.UNSPECIFIED);
		this.supportChooseMethods = ret;
		return ret;
	}

	@Override
	public PlaybackOrder getDefaultChooseMethod() {
		return PlaybackOrder.RANDOM;
	}

	@Override
	public MediaItem chooseItem(final PlaybackOrder order, final MediaItem previousItem) throws MorriganException {
		final ChooseMediaRequest.Builder req = ChooseMediaRequest.newBuilder()
				.setQuery(this.listRef.getSearch())
				.setCount(1);
		switch (order) {
		case RANDOM:
			req.setMethod(ChooseMethod.RANDOM);
			break;
		case BYLASTPLAYED:
			req.setMethod(ChooseMethod.LESS_RECENT);
			break;
		case BYSTARTCOUNT:
			req.setMethod(ChooseMethod.LESS_PLAYED);
			break;
		default:
			throw new IllegalArgumentException("Unsupported method: " + order);
		}

		try {
			final ChooseMediaReply resp = blockingStub().chooseMedia(req.build());
			if (resp.getItemCount() < 1) return null;
			return makeItem(resp.getItem(0));
		}
		catch (final StatusRuntimeException e) {
			throw new MorriganException("chooseMedia() RPC failed: " + e.toString(), e);
		}
	}

}
