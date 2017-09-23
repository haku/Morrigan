package com.vaguehope.morrigan.android.playback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.vaguehope.morrigan.android.helper.LogWrapper;

public class PlaybackClient {

	private final LogWrapper log = new LogWrapper();
	private final Context context;
	private Runnable serviceIsReady = null;
	private Playbacker boundService;
	private boolean boundToService = false;

	public PlaybackClient (final Context context, final String name) {
		this(context, name, null);
	}

	public PlaybackClient (final Context context, final String name, final Runnable serviceIsReady) {
		this.context = context;
		this.log.setPrefix(name);
		this.serviceIsReady = serviceIsReady;
		bindServiceService();
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
		this.serviceIsReady = null;
	}

	public Playbacker getService () {
		return this.boundService;
	}

	protected LogWrapper getLog () {
		return this.log;
	}

	protected void callServiceReadyListener () {
		if (this.serviceIsReady != null) this.serviceIsReady.run();
	}

	protected void setBoundServiceService (final Playbacker boundServiceService) {
		this.boundService = boundServiceService;
	}

	private final ServiceConnection mServiceServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected (final ComponentName className, final IBinder service) {
			setBoundServiceService(((PlaybackService.LocalBinder) service).getService());
			if (getService() == null) getLog().e("Got service call back, but mBoundServiceService==null.  Expect further errors.");
			callServiceReadyListener();
		}

		@Override
		public void onServiceDisconnected (final ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			setBoundServiceService(null);
			getLog().w("ServiceService unexpectadly disconnected.");
		}

	};

	private void bindServiceService () {
		this.boundToService = this.context.bindService(new Intent(this.context, PlaybackService.class), this.mServiceServiceConnection, Context.BIND_AUTO_CREATE);
		if (!this.boundToService) {
			this.log.e("Failed to bind to PlaybackService.  Expect further errors.");
		}
	}

	private void unbindServiceService () {
		if (this.boundToService && this.boundService != null) {
			try {
				this.context.unbindService(this.mServiceServiceConnection);
			}
			catch (final Exception e) {
				this.log.e("Exception caught in unbindServiceService().", e);
			}
		}
		this.boundService = null;
	}

}
