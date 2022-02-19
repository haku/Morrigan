package com.vaguehope.morrigan.dlna.content;

import com.vaguehope.morrigan.model.media.MediaListReference;

public class MlrAnd<T> {

	private final MediaListReference mlr;

	private final T obj;

	public MlrAnd (final MediaListReference mlr, final T obj) {
		this.mlr = mlr;
		this.obj = obj;
	}

	public MediaListReference getMlr () {
		return this.mlr;
	}

	public T getObj () {
		return this.obj;
	}

	@Override
	public String toString () {
		return String.format("MlrAnd{%s, %s}", this.mlr, this.obj);
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.mlr == null) ? 0 : this.mlr.hashCode());
		result = prime * result + ((this.obj == null) ? 0 : this.obj.hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object other) {
		if (this == other) return true;
		if (other == null) return false;
		if (!(other instanceof MlrAnd<?>)) return false;
		final MlrAnd<?> that = (MlrAnd<?>) other;

		if (this.mlr == null) {
			if (that.mlr != null) return false;
		}
		else if (!this.mlr.equals(that.mlr)) {
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
