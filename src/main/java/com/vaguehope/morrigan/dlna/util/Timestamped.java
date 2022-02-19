package com.vaguehope.morrigan.dlna.util;

import java.util.concurrent.TimeUnit;

public class Timestamped<T> {

	private static final long AGE_OF_OLD_NANOS = TimeUnit.HOURS.toNanos(1);

	public static <T> Timestamped<T> of(final T v) {
		return new Timestamped<T>(System.nanoTime(), v);
	}

	public static <T> Timestamped<T> old(final T v) {
		return new Timestamped<T>(System.nanoTime() - AGE_OF_OLD_NANOS, v);
	}

	private final long nanos;
	private final T v;

	private Timestamped (final long nanos, final T v) {
		if (v == null) throw new IllegalArgumentException("v can not be null.");
		this.nanos = nanos;
		this.v = v;
	}

	public long age (final TimeUnit units) {
		return units.convert(System.nanoTime() - this.nanos, TimeUnit.NANOSECONDS);
	}

	public T get () {
		return this.v;
	}

	@Override
	public String toString () {
		return String.format("Timestamped{%s @%ss}", this.v, age(TimeUnit.SECONDS));
	}

}
