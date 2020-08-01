package com.vaguehope.morrigan.android;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public abstract class AwakeService extends IntentService {

	public AwakeService (final String name) {
		super(name);
	}

	@Override
	protected final void onHandleIntent (final Intent i) {
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		final WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.LOGTAG);
		wl.acquire();
		try {
			doWork(i);
		}
		finally {
			wl.release();
		}
	}

	protected abstract void doWork (Intent i);

}
