package net.sparktank.morrigan.playback;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import net.sparktank.morrigan.config.Config;

public class PlaybackEngineFactory {
	
	public static IPlaybackEngine makePlaybackEngine () throws ImplException {
		try {
			File[] files = Config.getPlaybackEngineJars();
			
			URL jarUrls[] = new URL [files.length];
			for (int i = 0; i < files.length; i++) {
//				System.out.println(files[i].getAbsolutePath());
				jarUrls[i] = new URL("jar", "", "file:" + files[i].getAbsolutePath() + "!/");
			}
			
			URLClassLoader classLoader = URLClassLoader.newInstance(jarUrls, IPlaybackEngine.class.getClassLoader());
			Class<?> c = classLoader.loadClass(Config.getPlaybackEngineClass());
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
