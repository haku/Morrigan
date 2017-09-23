package com.vaguehope.morrigan.android.playback;

import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class PlaybackService extends Service implements Playbacker {
	// Service.

	@Override
	public void onCreate () {
		super.onCreate();
		instanceStart();
	}

	@Override
	public void onDestroy () {
		instanceStop();
		super.onDestroy();
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
	}

	private void instanceStop () {
		this.playbackInstance.dispose();
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
