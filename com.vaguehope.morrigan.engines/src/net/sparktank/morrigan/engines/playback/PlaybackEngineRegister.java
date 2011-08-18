package net.sparktank.morrigan.engines.playback;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class PlaybackEngineRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final Logger logger = Logger.getLogger(PlaybackEngineRegister.class.getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	static private ConcurrentMap<String, PlaybackEngineFactory> engineFactories = new ConcurrentHashMap<String, PlaybackEngineFactory>();
	
	static public void registerFactory (String id, PlaybackEngineFactory factory) {
		PlaybackEngineFactory r = engineFactories.putIfAbsent(id, factory);
		if (r != null) throw new IllegalArgumentException("ID '"+id+"' already in use.");
		logger.info("Playback engine factory registered: '"+id+"'.");
	}
	
	static public void unregisterFactory (String id) {
		engineFactories.remove(id);
	}
	
	/**
	 * TODO add much better error handling.
	 * @return
	 */
	static public IPlaybackEngine getNewPlaybackEngine () {
		if (engineFactories.size() < 1) {
			return null;
		}
		
		for (Entry<String, PlaybackEngineFactory> entry : engineFactories.entrySet()) {
			try {
				IPlaybackEngine engine = entry.getValue().getNewPlaybackEngine();
				return engine;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
