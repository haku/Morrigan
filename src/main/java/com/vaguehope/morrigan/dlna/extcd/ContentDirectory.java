package com.vaguehope.morrigan.dlna.extcd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.support.contentdirectory.callback.Browse;
import org.jupnp.support.contentdirectory.callback.Search;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.Res;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaNode;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.Cache;
import com.vaguehope.morrigan.util.Quietly;
import com.vaguehope.morrigan.util.StringHelper;

public class ContentDirectory {

	static final String SEARCH_BY_ID_PREFIX = "id=";
	private static final String ROOT_CONTENT_ID = "0"; // Root id of '0' is in the spec.
	private static final String TYPE_CRITERIA = "(upnp:class derivedfrom \"object.item.videoItem\" or upnp:class derivedfrom \"object.item.audioItem\")";

	private static final long SLEEP_BEFORE_RETRY_MILLIS = 500L;
	private static final int ACTION_TIMEOUT_SECONDS = 10;
	private static final Logger LOG = LoggerFactory.getLogger(ContentDirectory.class);

	private final ControlPoint controlPoint;
	private final RemoteService contentDirectory;
	private final MetadataStorage metadataStorage;

	private final Cache<String, IMediaItem> itemCache = new Cache<>(100);

	public ContentDirectory (final ControlPoint controlPoint, final RemoteService contentDirectory, final MetadataStorage metadataStorage) {
		this.controlPoint = controlPoint;
		this.contentDirectory = contentDirectory;
		this.metadataStorage = metadataStorage;
	}

	public IMediaItem fetchItemByIdWithRetry (final String remoteId, final int maxTries) throws DbException {
		int attempt = 0;
		while (true) {
			attempt += 1;
			try {
				return fetchItemById(remoteId);
			}
			catch (final RuntimeException e) {
				if (attempt >= maxTries) throw e;
			}
			catch (final DbException e) {
				if (attempt >= maxTries) throw e;
			}
			Quietly.sleep(SLEEP_BEFORE_RETRY_MILLIS);
		}
	}

	public List<IMediaItem> searchWithRetry (final String term, final int maxResults, final int maxTries) throws DbException {
		int attempt = 0;
		while (true) {
			attempt += 1;
			try {
				return search(term, maxResults);
			}
			catch (final RuntimeException e) {
				if (attempt >= maxTries) throw e;
			}
			catch (final DbException e) {
				if (attempt >= maxTries) throw e;
			}
			Quietly.sleep(500L);
		}
	}

	public IMediaItem fetchItemById (final String remoteId) throws DbException {
		final IMediaItem cached = this.itemCache.getFresh(remoteId, 1, TimeUnit.MINUTES);
		if (cached != null) return cached;

		final CountDownLatch cdl = new CountDownLatch(1);
		final SyncBrowse req = new SyncBrowse(cdl, this.contentDirectory, remoteId, BrowseFlag.METADATA, Browse.CAPS_WILDCARD, 0, 2L);
		this.controlPoint.execute(req);

		await(cdl, "Fetch '%s' from content directory '%s'.", remoteId, this.contentDirectory);
		if (req.getRef() == null) throw new DbException(req.getErr());

		final List<Item> items = req.getRef().getItems();
		if (items.size() < 1) return null;
		if (items.size() > 1) LOG.warn("Fetching item {} returned more than 1 result.", remoteId);
		final IMediaItem item = didlItemToMnItem(items.get(0));
		this.itemCache.put(remoteId, item);
		return item;
	}

	public List<IMediaItem> search (final String term, final int maxResults) throws DbException {
		if (StringHelper.blank(term) || "*".equals(term)) {
			return oldDlnaBrowseRoot(maxResults);
		}
		else if (term.startsWith(SEARCH_BY_ID_PREFIX)) {
			return oldDlnaBrowse(StringHelper.removeStart(term, SEARCH_BY_ID_PREFIX), maxResults);
		}
		return dlnaSearch(term, maxResults);
	}

	public MediaNode fetchRootContainer (final int maxResults) throws MorriganException {
		return fetchContainer(ROOT_CONTENT_ID, maxResults);
	}

	public MediaNode fetchContainer (final String containerId, final int maxResults) throws MorriganException {
		final CountDownLatch cdl = new CountDownLatch(2);
		final SyncBrowse mdReq = new SyncBrowse(cdl, this.contentDirectory, containerId, BrowseFlag.METADATA, Browse.CAPS_WILDCARD, 0, 1L);
		final SyncBrowse dcReq = new SyncBrowse(cdl, this.contentDirectory, containerId, BrowseFlag.DIRECT_CHILDREN, Browse.CAPS_WILDCARD, 0, (long) maxResults);
		this.controlPoint.execute(mdReq);
		this.controlPoint.execute(dcReq);

		await(cdl, "Browse '%s' on content directory '%s'.", containerId, this.contentDirectory);
		if (mdReq.getRef() == null) throw new MorriganException(mdReq.getErr());
		if (dcReq.getRef() == null) throw new MorriganException(dcReq.getErr());

		if (mdReq.getRef().getContainers().size() < 1) {
			throw new MorriganException("Container not found: " + containerId);
		}

		final Container cont = mdReq.getRef().getContainers().get(0);
		final String nodeTitle = cont.getTitle();
		final String parentId = "-1".equals(cont.getParentID()) ? null : cont.getParentID();

		final List<MediaNode> nodes = didlContainersToNodes(dcReq.getRef().getContainers());
		final List<IMediaItem> items = didlItemsToMnItems(dcReq.getRef().getItems());

		return new MediaNode(containerId, nodeTitle, parentId, nodes, items);
	}

	private List<IMediaItem> oldDlnaBrowseRoot (final int maxResults) throws DbException {
		return oldDlnaBrowse(ROOT_CONTENT_ID, maxResults);
	}

	private List<IMediaItem> oldDlnaBrowse (final String containerId, final int maxResults) throws DbException {
		final CountDownLatch cdl = new CountDownLatch(2);
		final SyncBrowse mdReq = new SyncBrowse(cdl, this.contentDirectory, containerId, BrowseFlag.METADATA, Browse.CAPS_WILDCARD, 0, 1L);
		final SyncBrowse dcReq = new SyncBrowse(cdl, this.contentDirectory, containerId, BrowseFlag.DIRECT_CHILDREN, Browse.CAPS_WILDCARD, 0, (long) maxResults);
		this.controlPoint.execute(mdReq);
		this.controlPoint.execute(dcReq);

		await(cdl, "Browse '%s' on content directory '%s'.", containerId, this.contentDirectory);
		if (mdReq.getRef() == null) throw new DbException(mdReq.getErr());
		if (dcReq.getRef() == null) throw new DbException(dcReq.getErr());

		final List<IMediaItem> ret = new ArrayList<>();
		if (mdReq.getRef().getContainers().size() > 0) {
			final String parentId = mdReq.getRef().getContainers().get(0).getParentID();
			if (!"-1".equals(parentId)) ret.add(new DidlContainer(parentId, ".."));
		}
		ret.addAll(didlContainersToMnItems(dcReq.getRef().getContainers()));
		ret.addAll(didlItemsToMnItems(dcReq.getRef().getItems()));
		return ret;
	}

	private List<IMediaItem> dlnaSearch (final String term, final int maxResults) throws DbException {
		final String searchCriteria = String.format("(%s and dc:title contains \"%s\")", TYPE_CRITERIA, term);

		final CountDownLatch cdl = new CountDownLatch(1);
		final AtomicReference<DIDLContent> ref = new AtomicReference<>();
		final AtomicReference<String> err = new AtomicReference<>();

		this.controlPoint.execute(new Search(this.contentDirectory, ROOT_CONTENT_ID, searchCriteria, Search.CAPS_WILDCARD, 0, (long) maxResults) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse operation, final String defaultMsg) {
				final String msg = "Failed to search content directory: " + defaultMsg;
				LOG.warn(msg);
				err.set(msg);
				cdl.countDown();
			}

			@Override
			public void received (final ActionInvocation<?> invocation, final DIDLContent didl) {
				ref.set(didl);
				cdl.countDown();
			}

			@Override
			public void updateStatus (final Status status) {
				// Unused.
			}
		});

		await(cdl, "Search '%s' on content directory '%s'.", term, this.contentDirectory);
		if (ref.get() == null) throw new DbException(err.get());
		return didlItemsToMnItems(ref.get().getItems());
	}

	private static List<MediaNode> didlContainersToNodes (final List<Container> containers) {
		final List<MediaNode> ret = new ArrayList<>();
		for (final Container c : containers) {
			ret.add(new MediaNode(c.getId(), c.getTitle(), c.getParentID(), null, null));
		}
		return ret;
	}

	private static List<IMediaItem> didlContainersToMnItems (final List<Container> containers) {
		final List<IMediaItem> ret = new ArrayList<>();
		for (final Container container : containers) {
			ret.add(new DidlContainer(container));
		}
		return ret;
	}

	private List<IMediaItem> didlItemsToMnItems (final List<Item> items) throws DbException {
		final List<IMediaItem> ret = new ArrayList<>();
		for (final Item item : items) {
			ret.add(didlItemToMnItem(item));
		}
		return ret;
	}

	private IMediaItem didlItemToMnItem (final Item item) throws DbException {
		Res primaryRes = null;
		MediaType mediaType = MediaType.UNKNOWN;
		Res artRes = null;
		for (final Res res : item.getResources()) {
			final String type = res.getProtocolInfo().getContentFormatMimeType().getType();
			if ("video".equalsIgnoreCase(type) || "audio".equalsIgnoreCase(type)) {
				if (primaryRes == null) {
					primaryRes = res;
					mediaType = MediaType.TRACK;
				}
			}
			else if ("image".equalsIgnoreCase(type)) {
				if (artRes == null) {
					artRes = res;
				}
			}
		}

		if (primaryRes == null && artRes != null) {
			primaryRes = artRes;
			mediaType = MediaType.PICTURE;
			artRes = null;
		}

		if (primaryRes == null) {
			final StringBuilder sb = new StringBuilder()
					.append("id=").append(item.getId())
					.append(" title=").append(item.getTitle());
			for (final Res res : item.getResources()) {
				sb.append(" res{").append(res.getValue())
						.append(", ").append(res.getProtocolInfo().getContentFormat())
						.append("}");
			}
			throw new IllegalArgumentException("No media res found for item: " + sb.toString());
		}

		final Metadata metadata = this.metadataStorage.getMetadataProxy(item.getId());

		return new DidlItem(item, primaryRes, mediaType, artRes, metadata);
	}

	private static void await (final CountDownLatch cdl, final String msgFormat, final Object... msgArgs) {
		try {
			if (cdl.await(ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) return;
			throw new IllegalStateException("Timed out while trying to " + String.format(msgFormat, msgArgs));
		}
		catch (final InterruptedException e) {
			throw new IllegalStateException("Interupted while trying to " + String.format(msgFormat, msgArgs), e);
		}
	}

}
