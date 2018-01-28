package com.vaguehope.morrigan.android.playback;

public interface Playbacker {

	void playPausePlayback();
	void stopPlayback();
	void gotoNextItem();

	PlayOrder getPlayOrder();
	void setPlayOrder(PlayOrder newOrder);

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

}
