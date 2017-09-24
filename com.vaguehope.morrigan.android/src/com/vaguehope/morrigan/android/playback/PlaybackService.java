package com.vaguehope.morrigan.android.playback;

import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

public class PlaybackService extends Service implements Playbacker {
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
		public Playbacker getService () {
			return PlaybackService.this;
		}
	}

	// Instance.

	private PlaybackInstance playbackInstance;

	private void instanceStart () {
		this.playbackInstance = new PlaybackInstance(this);
		startForeground(
				this.playbackInstance.getNotificationId(),
				this.playbackInstance.getNotif());
	}

	private void instanceStop () {
		this.playbackInstance.dispose();
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
				break;
			default:
				this.playbackInstance.onBroadcastAction(actionCode);
		}
	}

	// Methods.

	@Override
	public List<QueueItem> getQueue () {
		return this.playbackInstance.getQueue();
	}

	@Override
	public void notifyQueueChanged () {
		this.playbackInstance.notifyQueueChanged();
	}

	@Override
	public void playPausePlayback () {
		this.playbackInstance.playPausePlayback();
	}

	@Override
	public void stopPlayback () {
		this.playbackInstance.stopPlayback();
	}

	@Override
	public void gotoNextItem () {
		this.playbackInstance.gotoNextItem();
	}

	@Override
	public void addPlaybackListener (final PlaybackWatcher watcher) {
		this.playbackInstance.addPlaybackListener(watcher);
	}

	@Override
	public void removePlaybackListener (final PlaybackWatcher watcher) {
		this.playbackInstance.removePlaybackListener(watcher);
	}

}
