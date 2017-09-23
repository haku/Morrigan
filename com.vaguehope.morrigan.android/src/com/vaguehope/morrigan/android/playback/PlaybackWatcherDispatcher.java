package com.vaguehope.morrigan.android.playback;

import java.util.Set;

import com.vaguehope.morrigan.android.playback.Playbacker.PlaybackWatcher;

public class PlaybackWatcherDispatcher implements PlaybackWatcher {

	private final Set<PlaybackWatcher> playbackWatchers;

	public PlaybackWatcherDispatcher (final Set<PlaybackWatcher> playbackWatchers) {
		this.playbackWatchers = playbackWatchers;
	}

	@Override
	public void queueChanged () {
		for (final PlaybackWatcher w : this.playbackWatchers) {
			w.queueChanged();
		}
	}

	@Override
	public void playbackLoading (final QueueItem item) {
		for (final PlaybackWatcher w : this.playbackWatchers) {
			w.playbackLoading(item);
		}
	}

	@Override
	public void playbackPlaying () {
		for (final PlaybackWatcher w : this.playbackWatchers) {
			w.playbackPlaying();
		}
	}

	@Override
	public void playbackPaused () {
		for (final PlaybackWatcher w : this.playbackWatchers) {
			w.playbackPaused();
		}
	}

	@Override
	public void playbackStopped () {
		for (final PlaybackWatcher w : this.playbackWatchers) {
			w.playbackStopped();
		}
	}

	@Override
	public void playbackError () {
		for (final PlaybackWatcher w : this.playbackWatchers) {
			w.playbackError();
		}
	}

}
