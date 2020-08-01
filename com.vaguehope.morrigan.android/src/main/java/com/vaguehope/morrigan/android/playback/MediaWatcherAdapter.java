package com.vaguehope.morrigan.android.playback;

import com.vaguehope.morrigan.android.playback.MediaDb.MediaWatcher;

public abstract class MediaWatcherAdapter implements MediaWatcher {

	@Override
	public void queueChanged () {}

	@Override
	public void librariesChanged () {}

}
