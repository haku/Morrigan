package com.vaguehope.morrigan.rpc.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.vaguehope.morrigan.model.media.MediaItem;

public class RpcItemCache {

	private static final Logger LOG = LoggerFactory.getLogger(RpcItemCache.class);

	// TODO maybe provide a way to GC this?  atm its unbounded growth.
	private final Cache<String, MediaItem> cache = CacheBuilder.newBuilder()
			.build();

	public void putAll(final Collection<MediaItem> items) {
		for (final MediaItem item : items) {
			put(item);
		}
	}

	public void put(final MediaItem item) {
		this.cache.put(item.getRemoteId(), item);
	}

	public MediaItem getForId(final String id) {
		if (id == null) return null;
		final MediaItem item = this.cache.getIfPresent(id);
		if (item == null) throw new IllegalStateException("Item cache does not have id: " + id);
		return item;
	}

	public List<MediaItem> getForIds(final Collection<String> ids) {
		final List<MediaItem> ret = new ArrayList<>();
		for (final String id : ids) {
			final MediaItem item = this.cache.getIfPresent(id);
			if (item == null) {
				LOG.warn("ID not found in item cache: {}", id);
				continue;
			}
			ret.add(item);
		}
		return ret;
	}

}
