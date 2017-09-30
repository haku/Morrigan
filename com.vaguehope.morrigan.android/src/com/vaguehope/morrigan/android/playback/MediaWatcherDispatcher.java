package com.vaguehope.morrigan.android.playback;

import java.util.Set;

import com.vaguehope.morrigan.android.playback.MediaDb.MediaWatcher;

public class MediaWatcherDispatcher implements MediaWatcher {

	private final Set<MediaWatcher> watchers;

	public MediaWatcherDispatcher (final Set<MediaWatcher> watchers) {
		this.watchers = watchers;
	}

	@Override
	public void librariesChanged () {
		for (final MediaWatcher w : this.watchers) {
			w.librariesChanged();
		}
	}

}
