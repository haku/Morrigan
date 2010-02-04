package net.sparktank.morrigan.playback;

import net.sparktank.morrigan.playback.IPlaybackEngine.PlayState;

public interface IPlaybackStatusListener {
	
	public void statusChanged (PlayState state);
	
	public void positionChanged (long position);
	
}
