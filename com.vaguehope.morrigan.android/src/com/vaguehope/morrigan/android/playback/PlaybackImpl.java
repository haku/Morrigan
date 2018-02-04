package com.vaguehope.morrigan.android.playback;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.playback.MediaDb.MediaWatcher;

public class PlaybackImpl implements Playbacker {

	private static final String PREF_PLAYORDER = "playorder";
	private static final String PREF_CURRENT_ITEM = "current_item";

	private static final LogWrapper LOG = new LogWrapper("PI");

	private final Context context;
	private final MediaDb mediaDb;
	private final MessageHandler messageHandler;
	private final SharedPreferences sharedPreferences;

	private final NotificationManager notifMgr;
	private Builder notif;

	private final Set<PlaybackWatcher> playbackWatchers = new CopyOnWriteArraySet<PlaybackWatcher>();
	private final PlaybackWatcherDispatcher playbackWatcherDispatcher = new PlaybackWatcherDispatcher(this.playbackWatchers);

	public PlaybackImpl (final Context context, final MediaDb mediaDb) {
		this.context = context;
		this.mediaDb = mediaDb;
		this.messageHandler = new MessageHandler(this);
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		this.notifMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		makeNotif();

		mediaDb.addMediaWatcher(this.queueWatcher);
		restoreCurrentItem();
	}

	/**
	 * Release all resources.
	 */
	public void dispose () {
		unloadPlayback();
		this.mediaDb.removeMediaWatcher(this.queueWatcher);
		this.notifMgr.cancel(PlaybackCodes.PLAYBACK_NOTIFICATION_ID);
	}

	protected PlaybackWatcherDispatcher getPlaybackWatcherDispatcher () {
		return this.playbackWatcherDispatcher;
	}

	// Notification.

	private void makeNotif () {
		final Intent showPlaybackI = new Intent(this.context, PlaybackActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		final PendingIntent showPlaybackPi = PendingIntent.getActivity(this.context, PlaybackCodes.ACTION_SHOW_UI, showPlaybackI, PendingIntent.FLAG_CANCEL_CURRENT);

		// TODO make custom notification.
		// https://stackoverflow.com/questions/27673943/notification-action-button-not-clickable-in-lock-screen

		this.notif = new Notification.Builder(this.context)
				.setContentIntent(showPlaybackPi)
				.setSmallIcon(R.drawable.stop)
				.setContentTitle("Morrigan Player")
				.setOngoing(true)
				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit",
						PlaybackBroadcastReceiver.makePendingIntent(this.context, PlaybackCodes.ACTION_EXIT))
				.addAction(R.drawable.pause, "Pause",
						PlaybackBroadcastReceiver.makePendingIntent(this.context, PlaybackCodes.ACTION_PLAY_PAUSE))
				.addAction(R.drawable.next, "Next",
						PlaybackBroadcastReceiver.makePendingIntent(this.context, PlaybackCodes.ACTION_NEXT));
		showNotif();
	}

	private void updateNotifTitle (final CharSequence msg) {
		this.notif.setContentTitle(msg);
		showNotif();
	}

	private void updateNotifSubtitle (final CharSequence msg) {
		this.notif.setContentText(msg);
		showNotif();
	}

	private void updateNotifLoadingIcon () {
		this.notif.setSmallIcon(R.drawable.next); // TODO better icon?
		showNotif();
	}

	private void updateNotifPlayIcon () {
		this.notif.setSmallIcon(R.drawable.play);
		showNotif();
	}

	private void updateNotifPauseIcon () {
		this.notif.setSmallIcon(R.drawable.pause);
		showNotif();
	}

	private void updateNotifStopIcon () {
		this.notif.setSmallIcon(R.drawable.stop);
		showNotif();
	}

	private void updateNotifErrorIcon () {
		this.notif.setSmallIcon(R.drawable.exclamation_red);
		showNotif();
	}

	private void showNotif () {
		this.notifMgr.notify(PlaybackCodes.PLAYBACK_NOTIFICATION_ID, this.notif.getNotification());
	}

	int getNotificationId () {
		return PlaybackCodes.PLAYBACK_NOTIFICATION_ID;
	}

	Notification getNotif () {
		return this.notif.getNotification();
	}

	// Watchers.

	@Override
	public void addPlaybackListener (final PlaybackWatcher watcher) {
		this.playbackWatchers.add(watcher);

		final Message msg = this.messageHandler.obtainMessage(Msgs.NOTIFY_NEW_WATCHER.ordinal());
		msg.obj = watcher;
		msg.sendToTarget();
	}

	@Override
	public void removePlaybackListener (final PlaybackWatcher watcher) {
		this.playbackWatchers.remove(watcher);
	}

	// Queue.

	private final MediaWatcher queueWatcher = new MediaWatcherAdapter() {
		@Override
		public void queueChanged () {
			redrawNotifSubtitle();
		}
	};

	private void redrawNotifSubtitle () {
		updateNotifSubtitle(String.format(
				"%s items in queue, %s.",
				PlaybackImpl.this.mediaDb.getQueueSize(),
				getPlayOrder()));
	}

	private QueueItem takeNextItemToPlay () {
		final QueueItem fromQueue = this.mediaDb.getFirstQueueItem();
		if (fromQueue != null) return fromQueue;

		final MediaItem mediaItem;
		switch (getPlayOrder()) {
			case RANDOM:
				mediaItem = this.mediaDb.randomMediaItem();
				break;
			case QUEUE_ONLY:
			default:
				mediaItem = null;
		}

		if (mediaItem != null) {
			return new QueueItem(this.context, mediaItem);
		}

		return null;
	}

	// Public playback.

	public void onBroadcastAction (final int actionCode) {
		switch (actionCode) {
			case PlaybackCodes.ACTION_PLAY_PAUSE:
				playPausePlayback();
				break;
			case PlaybackCodes.ACTION_NEXT:
				gotoNextItem();
				break;
			default:
		}
	}

	@Override
	public void playPausePlayback () {
		this.messageHandler.sendEmptyMessage(Msgs.PLAY_PAUSE.ordinal());
	}

	@Override
	public void stopPlayback () {
		this.messageHandler.sendEmptyMessage(Msgs.STOP.ordinal());
	}

	@Override
	public void gotoNextItem () {
		this.messageHandler.sendEmptyMessage(Msgs.GOTO_NEXT_ITEM.ordinal());
	}

	private volatile PlayOrder playOrder;

	@Override
	public PlayOrder getPlayOrder () {
		if (this.playOrder == null) {
			final String fromPref = this.sharedPreferences.getString(PREF_PLAYORDER, null);
			if (fromPref != null) {
				this.playOrder = PlayOrder.valueOf(fromPref);
			}
			else {
				this.playOrder = PlayOrder.QUEUE_ONLY;
			}
		}
		return this.playOrder;
	}

	@Override
	public void setPlayOrder (final PlayOrder newOrder) {
		final Editor edit = this.sharedPreferences.edit();
		edit.putString(PREF_PLAYORDER, newOrder.name());
		edit.commit();
		this.playOrder = newOrder;
		this.playbackWatcherDispatcher.playOrderChanged();
		redrawNotifSubtitle();
	}

	private void saveCurrentItem () {
		final String uri = this.currentItem != null ? this.currentItem.getUri().toString() : null;
		final Editor edit = this.sharedPreferences.edit();
		edit.putString(PREF_CURRENT_ITEM, uri);
		edit.commit();
	}

	private void restoreCurrentItem () {
		final String fromPref = this.sharedPreferences.getString(PREF_CURRENT_ITEM, null);
		if (fromPref != null) {
			try {
				final Uri uri = Uri.parse(fromPref);
				setCurrentItem(new QueueItem(this.context, uri));
			}
			catch (final Exception e) {
				LOG.e("Failed to restore current item: " + fromPref, e);
			}
		}
	}

	// Messages.

	private enum Msgs {
		PLAY_PAUSE,
		STOP,
		GOTO_NEXT_ITEM,
		NOTIFY_NEW_WATCHER;
		public static final Msgs values[] = values(); // Optimisation to avoid new array every time.
	}

	private static class MessageHandler extends Handler {

		private final WeakReference<PlaybackImpl> parentRef;

		public MessageHandler (final PlaybackImpl parent) {
			this.parentRef = new WeakReference<PlaybackImpl>(parent);
		}

		@Override
		public void handleMessage (final Message msg) {
			final PlaybackImpl parent = this.parentRef.get();
			if (parent != null) parent.msgOnMainThread(msg);
		}
	}

	protected void msgOnMainThread (final Message msg) {
		final Msgs m = Msgs.values[msg.what];
		LOG.i("msg.what=%s", m);
		try {
			switch (m) {
				case PLAY_PAUSE:
					startPlaybackOrPause();
					break;
				case STOP:
					unloadPlayback();
					break;
				case GOTO_NEXT_ITEM:
					startPlayingNextItem();
					break;
				case NOTIFY_NEW_WATCHER:
					notifyNewWatcher((PlaybackWatcher) msg.obj);
					break;
				default:
			}
		}
		catch (final Exception e) {
			// TODO show to user somehow?  Perhaps via an error listener?
			LOG.e("Playback action failed.", e);
		}
	}

	// Internal playback.
	// Must only be called via message handler, so effectively single threaded.

	private QueueItem currentItem;
	private MediaPlayer mediaPlayer;
	/**
	 * True if paused, false for all other states.
	 */
	private boolean isPaused;

	private void resetMediaPlayer () {
		if (this.mediaPlayer == null) {
			this.mediaPlayer = new MediaPlayer();
		}
		else {
			this.mediaPlayer.stop();
			this.mediaPlayer.reset();
		}

		this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		this.mediaPlayer.setWakeMode(this.context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
		this.isPaused = false;
	}

	/**
	 * If true mediaPlayer is not null.
	 */
	private boolean isMediaPlayerPlaying () {
		if (this.mediaPlayer == null) return false;
		return this.mediaPlayer.isPlaying();
	}

	/**
	 * If true mediaPlayer is not null.
	 */
	private boolean isMediaPlayerPaused () {
		if (this.mediaPlayer == null) return false;
		return this.isPaused;
	}

	private void startPlaybackOrPause () throws IOException {
		if (isMediaPlayerPlaying()) {
			this.mediaPlayer.pause();
			this.isPaused = true;
			LOG.i("Playback paused.");
			updateNotifPauseIcon();
			this.playbackWatcherDispatcher.playbackPaused();
		}
		else if (isMediaPlayerPaused()) {
			this.mediaPlayer.start();
			this.isPaused = false;
			LOG.i("Playback resumed.");
			updateNotifPlayIcon();
			this.playbackWatcherDispatcher.playbackPlaying();
		}
		else {
			QueueItem item = this.currentItem;
			if (item == null) item = takeNextItemToPlay();
			if (item == null) return;

			setCurrentItem(item);
			loadAndPlayCurrentItem();
		}
	}

	private void startPlayingNextItem () throws IOException {
		final QueueItem item = takeNextItemToPlay();
		if (item == null) return;

		setCurrentItem(item);
		loadAndPlayCurrentItem();
	}

	private void setCurrentItem (final QueueItem item) {
		this.currentItem = item;
		updateNotifTitle(item.getTitle());
	}

	private void loadAndPlayCurrentItem () throws IOException {
		final QueueItem item = this.currentItem;
		if (item == null) return;

		updateNotifLoadingIcon();
		this.playbackWatcherDispatcher.playbackLoading(item);

		boolean success = false;
		try {
			resetMediaPlayer();
			this.mediaPlayer.setDataSource(this.context.getApplicationContext(), item.getUri());
			this.mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion (final MediaPlayer mp) {
					LOG.i("Playback complete.");
					gotoNextItem(); // Go via message dispatched to get correct threading.
				}
			});

			// TODO make use of this.
			//this.mediaPlayer.setOnInfoListener(listener);

			// TODO can make use of this?
			//this.mediaPlayer.setNextMediaPlayer(next);

			this.mediaPlayer.prepare(); // TODO worth switch to async?
			this.mediaPlayer.start();
			this.isPaused = false;

			LOG.i("Playing: %s", item);
			success = true;
			updateNotifPlayIcon();
			this.playbackWatcherDispatcher.playbackPlaying();

			saveCurrentItem();
			if (item.hasRowId()) {
				this.mediaDb.removeFromQueue(Collections.singleton(item));
			}
		}
		finally {
			if (!success) {
				updateNotifErrorIcon();
				this.playbackWatcherDispatcher.playbackError();
			}
		}
	}

	private void unloadPlayback () {
		if (this.mediaPlayer != null) {
			this.mediaPlayer.stop();
			this.mediaPlayer.release();
			this.mediaPlayer = null;
		}

		updateNotifStopIcon();
		this.playbackWatcherDispatcher.playbackStopped();
	}

	private void notifyNewWatcher (final PlaybackWatcher w) {
		final QueueItem item = this.currentItem;
		if (item != null) w.playbackLoading(item);

		if (isMediaPlayerPlaying()) {
			w.playbackPlaying();
		}
		else if (isMediaPlayerPaused()) {
			w.playbackPaused();
		}
		else {
			w.playbackStopped();
		}
	}

}
