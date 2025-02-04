package com.vaguehope.morrigan.player;

import java.util.Collection;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;

public interface PlayerRegister extends PlayerReader {

	String nextIndex(String prefix);
	void register(Player target);
	void unregister(Player target);

	Collection<Player> getAll ();
	Player get (String id);

	/**
	 * Note: playbackEngineFactory will be disposed with player shutdown.
	 */
	Player makeLocal(String prefix, String name, PlaybackEngineFactory playbackEngineFactory);

	Player make(String id, String name, PlaybackEngineFactory playbackEngineFactory);

}
