package com.vaguehope.morrigan.android.playback;

public interface Playbacker {

	void playPausePlayback();
	void pausePlayback();
	void stopPlayback();
	void gotoNextItem();

	PlayOrder getPlayOrder();
	void setPlayOrder(PlayOrder newOrder);

	/**
	 * Volume as linear percentage from 0 to 100.
	 */
	int getVolume();
	void setVolume(int percentage);

	void addPlaybackListener (PlaybackWatcher watcher);
	void removePlaybackListener (PlaybackWatcher watcher);

	public interface PlaybackWatcher {
		void playbackLoading(QueueItem item);
		void playbackPlaying();
		void playbackPaused();
		void playbackStopped();
		void playbackError();
		void playOrderChanged();
		void exitRequested();
	}

	enum PlayOrder {
		QUEUE_ONLY,
		RANDOM,
	}

	void exitIfIdle();

}
