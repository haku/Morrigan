package net.sparktank.morrigan.playback;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import net.sparktank.morrigan.config.Config;

public class PlaybackEngineFactory {
	
	private static Map<String, IPlaybackEngine> engineCache = new HashMap<String, IPlaybackEngine>();
	
	public static IPlaybackEngine makePlaybackEngine () throws ImplException {
		try {
			String playbackEngineClass = Config.getPlaybackEngineClass();
			
			if (engineCache.containsKey(playbackEngineClass)) {
				return engineCache.get(playbackEngineClass);
			}
			
			File[] files = Config.getPlaybackEngineJars();
			
			URL jarUrls[] = new URL [files.length];
			for (int i = 0; i < files.length; i++) {
				jarUrls[i] = new URL("jar", "", "file:" + files[i].getAbsolutePath() + "!/");
				System.out.println("loaded jarUrl=" + files[i].getAbsolutePath());
			}
			
			URLClassLoader classLoader = URLClassLoader.newInstance(jarUrls, IPlaybackEngine.class.getClassLoader());
			Class<?> c = classLoader.loadClass(playbackEngineClass);
			IPlaybackEngine playbackEngine = (IPlaybackEngine) c.newInstance();
			
			playbackEngine.setClassPath(Config.getPlaybackEngineJarPaths());
			System.out.println("About " + playbackEngineClass + ":\n" + playbackEngine.getAbout());
			
			engineCache.put(playbackEngineClass, playbackEngine);
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
		
//		String[] supportedFormats = makePlaybackEngine().getSupportedFormats();
		
		// FIXME need to re-use existing engine, or put this list somewhere else.
		String formats = "mp3|ogg|wma|wmv|avi|mpg|mpeg|ac3|mp4|wav|ra|mpga|mkv|ogm|mpc|m4a|flv|rmvb";
		String[] supportedFormats = formats.split("\\|");
		
		for (int i = 0; i < supportedFormats.length; i++) {
			supportedFormats[i] = supportedFormats[i].toLowerCase();
		}
		
		return supportedFormats;
	}
	
}
