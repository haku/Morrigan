package net.sparktank.morrigan.playback;

import net.sparktank.morrigan.playback.IPlaybackEngine.PlayState;

public interface IPlaybackStatusListener {
	
	public void statusChanged (PlayState state);
	
	public void positionChanged (long position);
	
	public void onEndOfTrack ();
	
	public void onKeyPress (int keyCode);
	
	public void onMouseClick (int button, int clickCount);
	
	public void onError (Exception e);
	
}
