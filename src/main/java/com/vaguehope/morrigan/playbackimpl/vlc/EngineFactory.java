package com.vaguehope.morrigan.playbackimpl.vlc;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;

public class EngineFactory implements PlaybackEngineFactory {

	private final VlcFactory vlcFactory;

	public EngineFactory(final VlcFactory vlcFactory) {
		this.vlcFactory = vlcFactory;
	}

	@Override
	public IPlaybackEngine newPlaybackEngine() {
		return new PlaybackEngine(this.vlcFactory);
	}

}
