package com.vaguehope.morrigan.android.helper;

public class Result<T> {

	private final boolean success;
	private final T data;
	private final Exception e;

	public Result (final T data) {
		if (data == null) throw new IllegalArgumentException("Missing arg: data.");
		this.success = true;
		this.data = data;
		this.e = null;
	}

	public Result (final Exception e) {
		if (e == null) throw new IllegalArgumentException("Missing arg: e.");
		this.success = false;
		this.data = null;
		this.e = e;
	}

	public boolean isSuccess () {
		return this.success;
	}

	public T getData () {
		return this.data;
	}

	public Exception getE () {
		return this.e;
	}

}
