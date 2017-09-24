package com.vaguehope.morrigan.android.playback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.vaguehope.morrigan.android.helper.LogWrapper;

public class MediaClient {

	private final LogWrapper log = new LogWrapper();
	private final Context context;
	private Runnable serviceIsReadyListener = null;
	private MediaServices boundService;
	private boolean boundToService = false;

	public MediaClient (final Context context, final String name) {
		this(context, name, null);
	}

	public MediaClient (final Context context, final String name, final Runnable serviceIsReadyListener) {
		this.context = context;
		this.log.setPrefix(name);
		this.serviceIsReadyListener = serviceIsReadyListener;
		startAndBindServiceService();
	}

	public void dispose () {
		clearReadyListener();
		unbindServiceService();
	}

	@Override
	protected void finalize () throws Throwable { // NOSONAR finalize throws Throwable.
		unbindServiceService();
		super.finalize();
	}

	public void clearReadyListener () {
		this.serviceIsReadyListener = null;
	}

	public MediaServices getService () {
		return this.boundService;
	}

	protected LogWrapper getLog () {
		return this.log;
	}

	protected void callServiceReadyListener () {
		if (this.serviceIsReadyListener != null) this.serviceIsReadyListener.run();
	}

	protected void setBoundServiceService (final MediaServices boundServiceService) {
		this.boundService = boundServiceService;
	}

	private final ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected (final ComponentName className, final IBinder service) {
			setBoundServiceService(((MediaService.LocalBinder) service).getService());
			if (getService() == null) getLog().e("Got service call back, but boundService==null.  Expect further errors.");
			callServiceReadyListener();
		}

		@Override
		public void onServiceDisconnected (final ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			setBoundServiceService(null);
			getLog().w("Service unexpectadly disconnected.");
		}

	};

	private void startAndBindServiceService () {
		final Intent intent = new Intent(this.context, MediaService.class);
		this.context.startService(intent);
		this.boundToService = this.context.bindService(intent, this.serviceConnection, Context.BIND_AUTO_CREATE);
		if (!this.boundToService) {
			this.log.e("Failed to bind to PlaybackService.  Expect further errors.");
		}
	}

	private void unbindServiceService () {
		if (this.boundToService && this.boundService != null) {
			try {
				this.context.unbindService(this.serviceConnection);
			}
			catch (final Exception e) {
				this.log.e("Exception caught in unbindServiceService().", e);
			}
		}
		this.boundService = null;
	}

}
