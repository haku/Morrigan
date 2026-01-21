package morrigan.engines.playback;

public interface PlaybackEngineFactory {

	IPlaybackEngine newPlaybackEngine ();
	void dispose();

}
