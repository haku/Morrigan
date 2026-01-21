package morrigan.rpc.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import morrigan.model.media.MediaItem;

public class RpcItemCache {

	private static final Logger LOG = LoggerFactory.getLogger(RpcItemCache.class);

	private final Cache<String, MediaItem> freshCache = CacheBuilder.newBuilder()
			.expireAfterWrite(1, TimeUnit.MINUTES)
			.build();
	// TODO maybe provide a way to GC this?  atm its unbounded growth.
	private final Cache<String, MediaItem> allCache = CacheBuilder.newBuilder()
			.build();

	public void putAll(final Collection<MediaItem> items) {
		for (final MediaItem item : items) {
			put(item);
		}
	}

	public void put(final MediaItem item) {
		this.allCache.put(item.getRemoteId(), item);
		this.freshCache.put(item.getRemoteId(), item);
	}

	/**
	 * maybe null.
	 */
	public MediaItem getIfFresh(final String id) {
		if (id == null) return null;
		return this.freshCache.getIfPresent(id);
	}

	/**
	 * never null.
	 */
	public MediaItem getForId(final String id) {
		if (id == null) return null;
		final MediaItem item = this.allCache.getIfPresent(id);
		if (item == null) throw new IllegalStateException("Item cache does not have id: " + id);
		return item;
	}

	public List<MediaItem> getForIds(final Collection<String> ids) {
		final List<MediaItem> ret = new ArrayList<>();
		for (final String id : ids) {
			final MediaItem item = this.allCache.getIfPresent(id);
			if (item == null) {
				LOG.warn("ID not found in item cache: {}", id);
				continue;
			}
			ret.add(item);
		}
		return ret;
	}

}
