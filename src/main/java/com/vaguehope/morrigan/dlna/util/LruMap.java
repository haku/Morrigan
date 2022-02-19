package com.vaguehope.morrigan.dlna.util;

import java.util.LinkedHashMap;

public class LruMap<K, V> extends LinkedHashMap<K, V> {

	private static final long serialVersionUID = -610834208932716056L;

	private final int maxSize;

	public LruMap (final int initialSize, final int maxSize) {
		super(initialSize, 0.75f, true);
		this.maxSize = maxSize;
	}

	@Override
	protected boolean removeEldestEntry (final java.util.Map.Entry<K, V> eldest) {
		return size() >= this.maxSize;
	}

}
