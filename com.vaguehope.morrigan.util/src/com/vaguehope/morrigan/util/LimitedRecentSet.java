package com.vaguehope.morrigan.util;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LimitedRecentSet<T> implements Iterable<T> {

	private final Deque<T> data = new LinkedList<T>();
	private final Map<T, AtomicInteger> frequencies = new HashMap<T, AtomicInteger>();
	private final int limit;

	public LimitedRecentSet (final int limit) {
		if (limit < 1) throw new IllegalArgumentException();
		this.limit = limit;
	}

	public synchronized void push (final T o) {
		incrementFrequency(o);

		if (this.data.size() > 0 && Objs.equals(o, this.data.getFirst())) return;

		this.data.remove(o);
		this.data.push(o);
		while (this.data.size() > this.limit) {
			final T r = this.data.removeLast();
			this.frequencies.remove(r);
		}
	}

	private void incrementFrequency (final T o) {
		AtomicInteger f = this.frequencies.get(o);
		if (f == null) {
			f = new AtomicInteger(0);
			this.frequencies.put(o, f);
		}
		f.incrementAndGet();
	}

	public synchronized int frequency (final T o) {
		final AtomicInteger f = this.frequencies.get(o);
		if (f == null) return 0;
		return f.get();
	}

	public synchronized List<T> all () {
		return new ArrayList<T>(this.data);
	}

	@Override
	public Iterator<T> iterator () {
		return this.data.iterator();
	}

}
