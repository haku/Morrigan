package com.vaguehope.morrigan.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<K, V> extends LinkedHashMap<K, V> {

	private static final long serialVersionUID = -128564628456862486L;

	private final int cacheSize;

	public LruCache (final int cacheSize) {
		super(16, 0.75f, true);
		this.cacheSize = cacheSize;
	}

	@Override
	protected boolean removeEldestEntry (final Map.Entry<K, V> eldest) {
		return size() > this.cacheSize;
	}

}
