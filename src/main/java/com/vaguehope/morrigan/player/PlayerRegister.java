package com.vaguehope.morrigan.player;

import java.util.Collection;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.model.Register;


public interface PlayerRegister extends Register<Player>, PlayerReader {

	Collection<Player> getAll ();
	Player get (String id);

	/**
	 * Note: playbackEngineFactory will be disposed with player shutdown.
	 */
	LocalPlayer makeLocal(String prefix, String name, PlaybackEngineFactory playbackEngineFactory);

	LocalPlayer makeLocalProxy(Player player);

}
