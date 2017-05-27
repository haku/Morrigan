package com.vaguehope.morrigan.util;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class LimitedRecentSet<T> implements Iterable<T> {

	private final Deque<T> data = new LinkedList<T>();
	private final int limit;

	public LimitedRecentSet (final int limit) {
		if (limit < 1) throw new IllegalArgumentException();
		this.limit = limit;
	}

	public synchronized void push (final T o) {
		if (this.data.size() > 0 && Objs.equals(o, this.data.getFirst())) return;

		this.data.remove(o);
		this.data.push(o);
		while (this.data.size() > this.limit) {
			this.data.removeLast();
		}
	}

	public synchronized List<T> all () {
		return new ArrayList<T>(this.data);
	}

	@Override
	public Iterator<T> iterator () {
		return this.data.iterator();
	}

}
