package com.vaguehope.morrigan.android.playback;

public interface Playbacker {

	void playPausePlayback();
	void stopPlayback();
	void gotoNextItem();

	void addPlaybackListener (PlaybackWatcher watcher);
	void removePlaybackListener (PlaybackWatcher watcher);

	public interface PlaybackWatcher {
		void playbackLoading(QueueItem item);
		void playbackPlaying();
		void playbackPaused();
		void playbackStopped();
		void playbackError();
		void exitRequested();
	}

}
