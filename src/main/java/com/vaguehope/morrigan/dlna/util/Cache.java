package com.vaguehope.morrigan.dlna.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Cache<K, V> {

	private final Map<K, Wrapper<V>> cache;

	public Cache (final int size) {
		this(size, size);
	}

	public Cache (final int initialSize, final int maxSize) {
		this.cache = Collections.synchronizedMap(new LruMap<K, Wrapper<V>>(initialSize, maxSize));
	}

	public void put (final K key, final V value) {
		this.cache.put(key, new Wrapper<V>(value));
	}

	public V getFresh (final K key, final int maxAge, final TimeUnit ageUnits) {
		final Wrapper<V> w = this.cache.get(key);
		if (w == null) return null;
		if (w.age(ageUnits) > maxAge) return null;
		return w.getValue();
	}

	public V getEvenIfExpired (final K key) {
		final Wrapper<V> w = this.cache.get(key);
		if (w == null) return null;
		return w.getValue();
	}

	private static class Wrapper<V> {
		private final V value;
		private final long created;

		public Wrapper (final V value) {
			this.value = value;
			this.created = now();
		}

		public V getValue () {
			return this.value;
		}

		public long age (final TimeUnit unit) {
			return unit.convert(now() - this.created, TimeUnit.NANOSECONDS);
		}

		private static final long NANO_ORIGIN = System.nanoTime();

		protected static long now () {
			return System.nanoTime() - NANO_ORIGIN;
		}

	}

}
