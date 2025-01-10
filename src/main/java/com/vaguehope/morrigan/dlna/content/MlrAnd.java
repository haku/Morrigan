package com.vaguehope.morrigan.dlna.content;

import com.vaguehope.morrigan.model.media.ListRefWithTitle;

public class MlrAnd<T> {

	private final ListRefWithTitle listRef;

	private final T obj;

	public MlrAnd (final ListRefWithTitle mlr, final T obj) {
		this.listRef = mlr;
		this.obj = obj;
	}

	public ListRefWithTitle getMlr () {
		return this.listRef;
	}

	public T getObj () {
		return this.obj;
	}

	@Override
	public String toString () {
		return String.format("MlrAnd{%s, %s}", this.listRef, this.obj);
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.listRef == null) ? 0 : this.listRef.hashCode());
		result = prime * result + ((this.obj == null) ? 0 : this.obj.hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object other) {
		if (this == other) return true;
		if (other == null) return false;
		if (!(other instanceof MlrAnd<?>)) return false;
		final MlrAnd<?> that = (MlrAnd<?>) other;

		if (this.listRef == null) {
			if (that.listRef != null) return false;
		}
		else if (!this.listRef.equals(that.listRef)) {
			return false;
		}

		if (this.obj == null) {
			if (that.obj != null) return false;
		}
		else if (!this.obj.equals(that.obj)) {
			return false;
		}

		return true;
	}

}
