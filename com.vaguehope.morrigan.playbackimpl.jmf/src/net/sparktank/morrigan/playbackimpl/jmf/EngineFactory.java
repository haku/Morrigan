package net.sparktank.morrigan.playbackimpl.jmf;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.engines.playback.PlaybackEngineFactory;

public class EngineFactory implements PlaybackEngineFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public IPlaybackEngine getNewPlaybackEngine() {
		return new PlaybackEngine();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
