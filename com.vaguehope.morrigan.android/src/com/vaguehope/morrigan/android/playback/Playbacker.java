package com.vaguehope.morrigan.android.playback;

import java.util.List;

public interface Playbacker {

	/**
	 * Live view of queue.
	 */
	List<QueueItem> getQueue();

	/**
	 * You must call this after modifying the queue.
	 */
	void notifyQueueChanged();

	void playPausePlayback();
	void stopPlayback();
	void gotoNextItem();

	void addPlaybackListener (PlaybackWatcher watcher);
	void removePlaybackListener (PlaybackWatcher watcher);

	public interface PlaybackWatcher {
		void queueChanged();
		void playbackLoading(QueueItem item);
		void playbackPlaying();
		void playbackPaused();
		void playbackStopped();
		void playbackError();
		void exitRequested();
	}

}
