package com.vaguehope.morrigan.dlna.extcd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.contentdirectory.callback.Search;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.util.Cache;
import com.vaguehope.morrigan.dlna.util.Quietly;
import com.vaguehope.morrigan.dlna.util.StringHelper;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

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

	private final Cache<String, IMixedMediaItem> itemCache = new Cache<String, IMixedMediaItem>(100);

	public ContentDirectory (final ControlPoint controlPoint, final RemoteService contentDirectory, final MetadataStorage metadataStorage) {
		this.controlPoint = controlPoint;
		this.contentDirectory = contentDirectory;
		this.metadataStorage = metadataStorage;
	}

	public IMixedMediaItem fetchItemByIdWithRetry (final String remoteId, final int maxTries) throws DbException {
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

	public List<IMixedMediaItem> searchWithRetry (final String term, final int maxResults, final int maxTries) throws DbException {
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

	public IMixedMediaItem fetchItemById (final String remoteId) throws DbException {
		final IMixedMediaItem cached = this.itemCache.getFresh(remoteId, 1, TimeUnit.MINUTES);
		if (cached != null) return cached;

		final CountDownLatch cdl = new CountDownLatch(1);
		final AtomicReference<DIDLContent> ref = new AtomicReference<DIDLContent>();
		final AtomicReference<String> err = new AtomicReference<String>();

		this.controlPoint.execute(new Browse(this.contentDirectory, remoteId, BrowseFlag.METADATA, Browse.CAPS_WILDCARD, 0, 2L) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse operation, final String defaultMsg) {
				final String msg = "Failed to fetch item " + remoteId + ": " + defaultMsg;
				LOG.warn(msg);
				err.set(msg);
				cdl.countDown();
			}

			@Override
			public void received (final ActionInvocation invocation, final DIDLContent didl) {
				ref.set(didl);
				cdl.countDown();
			}

			@Override
			public void updateStatus (final Status status) {
				// Unused.
			}
		});
		await(cdl, "Fetch '%s' from content directory '%s'.", remoteId, this.contentDirectory);
		if (ref.get() == null) throw new DbException(err.get());

		final List<Item> items = ref.get().getItems();
		if (items.size() < 1) return null;
		if (items.size() > 1) LOG.warn("Fetching item {} returned more than 1 result.", remoteId);
		final IMixedMediaItem item = didlItemToMnItem(items.get(0));
		this.itemCache.put(remoteId, item);
		return item;
	}

	public List<IMixedMediaItem> search (final String term, final int maxResults) throws DbException {
		if (StringHelper.blank(term) || "*".equals(term)) {
			return dlnaBrowse(maxResults);
		}
		else if (term.startsWith(SEARCH_BY_ID_PREFIX)) {
			return dlnaBrowse(StringHelper.removeStart(term, SEARCH_BY_ID_PREFIX), maxResults);
		}
		return dlnaSearch(term, maxResults);
	}

	private List<IMixedMediaItem> dlnaBrowse (final int maxResults) throws DbException {
		return dlnaBrowse(ROOT_CONTENT_ID, maxResults);
	}

	private List<IMixedMediaItem> dlnaBrowse (final String containerId, final int maxResults) throws DbException {
		final CountDownLatch cdl = new CountDownLatch(2);
		final AtomicReference<DIDLContent> refMetadata = new AtomicReference<DIDLContent>();
		final AtomicReference<DIDLContent> refChildren = new AtomicReference<DIDLContent>();
		final AtomicReference<String> errMetadata = new AtomicReference<String>();
		final AtomicReference<String> errChildren = new AtomicReference<String>();

		this.controlPoint.execute(new Browse(this.contentDirectory, containerId, BrowseFlag.METADATA, Browse.CAPS_WILDCARD, 0, 1L) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse operation, final String defaultMsg) {
				final String msg = "Failed to get container metadata: " + defaultMsg;
				LOG.warn(msg);
				errMetadata.set(msg);
				cdl.countDown();
			}

			@Override
			public void received (final ActionInvocation invocation, final DIDLContent didl) {
				refMetadata.set(didl);
				cdl.countDown();
			}

			@Override
			public void updateStatus (final Status status) {
				// Unused.
			}
		});

		this.controlPoint.execute(new Browse(this.contentDirectory, containerId, BrowseFlag.DIRECT_CHILDREN, Browse.CAPS_WILDCARD, 0, (long) maxResults) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse operation, final String defaultMsg) {
				final String msg = "Failed to browse container: " + defaultMsg;
				LOG.warn(msg);
				errChildren.set(msg);
				cdl.countDown();
			}

			@Override
			public void received (final ActionInvocation invocation, final DIDLContent didl) {
				refChildren.set(didl);
				cdl.countDown();
			}

			@Override
			public void updateStatus (final Status status) {
				// Unused.
			}
		});

		await(cdl, "Browse '%s' on content directory '%s'.", containerId, this.contentDirectory);
		if (refMetadata.get() == null) throw new DbException(errMetadata.get());
		if (refChildren.get() == null) throw new DbException(errChildren.get());

		final List<IMixedMediaItem> ret = new ArrayList<IMixedMediaItem>();
		if (refMetadata.get().getContainers().size() > 0) {
			final String parentId = refMetadata.get().getContainers().get(0).getParentID();
			if (!"-1".equals(parentId)) ret.add(new DidlContainer(parentId, ".."));
		}
		ret.addAll(didlContainersToMnItems(refChildren.get().getContainers()));
		ret.addAll(didlItemsToMnItems(refChildren.get().getItems()));
		return ret;
	}

	private List<IMixedMediaItem> dlnaSearch (final String term, final int maxResults) throws DbException {
		final String searchCriteria = String.format("(%s and dc:title contains \"%s\")", TYPE_CRITERIA, term);

		final CountDownLatch cdl = new CountDownLatch(1);
		final AtomicReference<DIDLContent> ref = new AtomicReference<DIDLContent>();
		final AtomicReference<String> err = new AtomicReference<String>();

		this.controlPoint.execute(new Search(this.contentDirectory, ROOT_CONTENT_ID, searchCriteria, Search.CAPS_WILDCARD, 0, (long) maxResults) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse operation, final String defaultMsg) {
				final String msg = "Failed to search content directory: " + defaultMsg;
				LOG.warn(msg);
				err.set(msg);
				cdl.countDown();
			}

			@Override
			public void received (final ActionInvocation invocation, final DIDLContent didl) {
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

	private static List<IMixedMediaItem> didlContainersToMnItems (final List<Container> containers) {
		final List<IMixedMediaItem> ret = new ArrayList<IMixedMediaItem>();
		for (final Container container : containers) {
			ret.add(new DidlContainer(container));
		}
		return ret;
	}

	private List<IMixedMediaItem> didlItemsToMnItems (final List<Item> items) throws DbException {
		final List<IMixedMediaItem> ret = new ArrayList<IMixedMediaItem>();
		for (final Item item : items) {
			ret.add(didlItemToMnItem(item));
		}
		return ret;
	}

	private IMixedMediaItem didlItemToMnItem (final Item item) throws DbException {
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
