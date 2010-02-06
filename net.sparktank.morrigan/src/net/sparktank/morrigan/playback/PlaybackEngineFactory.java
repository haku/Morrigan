package net.sparktank.morrigan.playback;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import net.sparktank.morrigan.config.Config;

public class PlaybackEngineFactory {
	
	public static IPlaybackEngine makePlaybackEngine () throws ImplException {
		try {
			File file = new File(Config.PLAYBACK_ENGINE_JAR);
			System.out.println(file.getAbsolutePath());
			URL jarfile = new URL("jar", "", "file:" + file.getAbsolutePath() + "!/");
			URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { jarfile }, IPlaybackEngine.class.getClassLoader());
			Class<?> c = classLoader.loadClass(Config.PLAYBACK_ENGINE);
			IPlaybackEngine playbackEngine = (IPlaybackEngine) c.newInstance();
			
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
