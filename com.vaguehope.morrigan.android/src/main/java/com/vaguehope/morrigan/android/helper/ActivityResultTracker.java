package com.vaguehope.morrigan.android.helper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Intent;

public class ActivityResultTracker {

	public interface ActivityResultCallback {
		void onActivityResult (final int resultCode, final Intent data);
	}

	private final AtomicInteger requestCodeCounter;
	private final Map<Integer, ActivityResultCallback> callbacks = new ConcurrentHashMap<Integer, ActivityResultCallback>();

	public ActivityResultTracker (final int requestCodeOffset) {
		this.requestCodeCounter = new AtomicInteger(requestCodeOffset);
	}

	public int registerCallback (final ActivityResultCallback callback) {
		final int rc = this.requestCodeCounter.incrementAndGet();
		this.callbacks.put(Integer.valueOf(rc), callback);
		return rc;
	}

	public void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
		final ActivityResultCallback callback = this.callbacks.remove(Integer.valueOf(requestCode));
		if (callback != null) callback.onActivityResult(resultCode, data);
	}

}
