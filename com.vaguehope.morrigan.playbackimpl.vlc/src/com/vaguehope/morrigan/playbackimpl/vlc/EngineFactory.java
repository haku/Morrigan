package com.vaguehope.morrigan.playbackimpl.vlc;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;

public class EngineFactory implements PlaybackEngineFactory {
	
	@Override
	public IPlaybackEngine newPlaybackEngine() {
		return new PlaybackEngine();
	}
	
}
