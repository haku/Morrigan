package com.vaguehope.morrigan.android.playback;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

public class MediaService extends Service implements MediaServices {
	// Service.

	@Override
	public void onCreate () {
		super.onCreate();
		instanceStart();
		registerBroadcastReceiver();
	}

	@Override
	public void onDestroy () {
		unregisterBroadcastReceiver();
		instanceStop();
		super.onDestroy();
	}

	@Override
	public int onStartCommand (final Intent intent, final int flags, final int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	public IBinder onBind (final Intent intent) {
		return this.mBinder;
	}

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public MediaServices getService () {
			return MediaService.this;
		}
	}

	// Instance.

	private MediaDbImpl mediaDbImpl;
	private PlaybackImpl playbackInstance;

	private void instanceStart () {
		this.mediaDbImpl = new MediaDbImpl(this);
		this.playbackInstance = new PlaybackImpl(this, this.mediaDbImpl);
		startForeground(
				this.playbackInstance.getNotificationId(),
				this.playbackInstance.getNotif());
	}

	private void instanceStop () {
		this.playbackInstance.dispose();
		this.mediaDbImpl.close();
	}

	// Broadcasts.

	private BroadcastReceiver receiver;

	private void registerBroadcastReceiver () {
		this.receiver = new PlaybackBroadcastReceiver(this);
		final IntentFilter filter = new IntentFilter();
		filter.addAction(PlaybackCodes.ACTION_PLAYBACK);
		registerReceiver(this.receiver, filter);
	}

	private void unregisterBroadcastReceiver () {
		unregisterReceiver(this.receiver);
	}

	public void onBroadcastAction (final int actionCode) {
		switch (actionCode) {
			case PlaybackCodes.ACTION_EXIT:
				stopSelf();
				this.playbackInstance.getPlaybackWatcherDispatcher().exitRequested();
				break;
			default:
				this.playbackInstance.onBroadcastAction(actionCode);
		}
	}

	// MediaServices.

	@Override
	public Playbacker getPlaybacker () {
		if (this.playbackInstance == null) throw new IllegalStateException("playbackInstance missing.");
		return this.playbackInstance;
	}

	@Override
	public MediaDb getMediaDb () {
		if (this.mediaDbImpl == null) throw new IllegalStateException("mediaDbImpl missing.");
		return this.mediaDbImpl;
	}

}
