package com.vaguehope.morrigan.engines.playback;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;

public interface IPlaybackStatusListener {

	void statusChanged (PlayState state);

	void positionChanged (long position);

	void durationChanged (int duration);

	void onEndOfTrack ();

	void onKeyPress (int keyCode);

	void onMouseClick (int button, int clickCount);

	void onError (Exception e);

}
