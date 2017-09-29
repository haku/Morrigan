package com.vaguehope.morrigan.android.playback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.vaguehope.morrigan.android.AwakeService;
import com.vaguehope.morrigan.android.C;
import com.vaguehope.morrigan.android.helper.LogWrapper;

public abstract class MediaBindingAwakeService extends AwakeService {

	private final LogWrapper log;
	private final CountDownLatch dbReadyLatch = new CountDownLatch(1);
	private MediaClient bndMc;

	public MediaBindingAwakeService (final String name, final LogWrapper log) {
		super(name);
		this.log = log;
	}

	protected LogWrapper getLog () {
		return this.log;
	}

	@Override
	public void onCreate () {
		super.onCreate();
		connectDb();
	}

	@Override
	public void onDestroy () {
		disconnectDb();
		super.onDestroy();
	}

	private void connectDb () {
		this.log.d("Binding DB service...");
		final CountDownLatch latch = this.dbReadyLatch;
		this.bndMc = new MediaClient(getApplicationContext(), this.log.getPrefix(), new Runnable() {
			@Override
			public void run () {
				latch.countDown();
				getLog().d("Media service bound.");
			}
		});
	}

	private void disconnectDb () {
		this.bndMc.dispose();
		this.log.d("Media service rebound.");
	}

	protected boolean waitForDbReady () {
		boolean dbReady = false;
		try {
			dbReady = this.dbReadyLatch.await(C.SERVICE_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
		catch (final InterruptedException e) {/**/}
		if (!dbReady) this.log.e("Aborting: Time out waiting for DB service to connect.");
		return dbReady;
	}

	protected MediaDb getMediaDb () {
		final MediaClient d = this.bndMc;
		if (d == null) throw new IllegalStateException("Service not bound.");
		return d.getService().getMediaDb();
	}

}
