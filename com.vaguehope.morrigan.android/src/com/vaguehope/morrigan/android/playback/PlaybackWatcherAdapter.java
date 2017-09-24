package com.vaguehope.morrigan.android.playback;

import com.vaguehope.morrigan.android.playback.Playbacker.PlaybackWatcher;

public abstract class PlaybackWatcherAdapter implements PlaybackWatcher {

	@Override
	public void queueChanged () {}

	@Override
	public void playbackLoading (final QueueItem item) {}

	@Override
	public void playbackPlaying () {}

	@Override
	public void playbackPaused () {}

	@Override
	public void playbackStopped () {}

	@Override
	public void playbackError () {}

}
