package net.sparktank.morrigan.playback;

import net.sparktank.morrigan.config.Config;

public class PlaybackEngineFactory {
	
	public static IPlaybackEngine makePlaybackEngine () throws ImplException {
		Class<?> [] classParm = null;
		Object [] objectParm = null;
		
		try {
			Class<?> cl = Class.forName(Config.PLAYBACK_ENGINE);
			java.lang.reflect.Constructor<?> co = cl.getConstructor(classParm);
			IPlaybackEngine playbackEngine = (IPlaybackEngine) co.newInstance(objectParm);
			
			return playbackEngine;
			
		} catch (Exception e) {
			throw new ImplException(e);
		}
	}
	
	/**
	 * Returns a list of the file extensions that can be played
	 * when this engine is loaded.
	 * This method may need to load instances of each playback engine,
	 * so use it sparingly.
	 * @return Array of lower-case strings without dots.  e.g. "mp3", "ogg".
	 * @throws ImplException 
	 */
	public static String[] getSupportedFormats () throws ImplException {
		/*
		 * TODO: when we support multiple playback engines, scan them
		 * all to build one big list.
		 */
		
		String[] supportedFormats = makePlaybackEngine().getSupportedFormats();
		
		for (int i = 0; i < supportedFormats.length; i++) {
			supportedFormats[i] = supportedFormats[i].toLowerCase();
		}
		
		return supportedFormats;
	}
	
}
