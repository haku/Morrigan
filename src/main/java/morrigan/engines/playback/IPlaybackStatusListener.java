package morrigan.engines.playback;

import morrigan.engines.playback.IPlaybackEngine.PlayState;

public interface IPlaybackStatusListener {

	void statusChanged (PlayState state, boolean isEndOfTrack);

	void positionChanged (long position);

	void durationChanged (int duration);

	void onError (Exception e);

}
