package net.sparktank.morrigan.playback;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;

public class PlaybackEngineFactory {
	
	private static URLClassLoader classLoader = null;
	
	private static URLClassLoader getClassLoader () throws MorriganException, MalformedURLException {
		if (classLoader == null) {
			File[] files = Config.getPlaybackEngineJars();
			
			URL jarUrls[] = new URL [files.length];
			for (int i = 0; i < files.length; i++) {
				jarUrls[i] = new URL("jar", "", "file:" + files[i].getAbsolutePath() + "!/");
				System.out.println("loaded jarUrl=" + files[i].getAbsolutePath());
			}
			
			classLoader = URLClassLoader.newInstance(jarUrls, IPlaybackEngine.class.getClassLoader());
		}
		return classLoader;
	}
	
	public static IPlaybackEngine makePlaybackEngine () throws ImplException {
		try {
			String playbackEngineClass = Config.getPlaybackEngineClass();
			Class<?> c = getClassLoader().loadClass(playbackEngineClass);
			IPlaybackEngine playbackEngine = (IPlaybackEngine) c.newInstance();
			
			playbackEngine.setClassPath(Config.getPlaybackEngineJarPaths());
			System.out.println("About " + playbackEngineClass + ":\n" + playbackEngine.getAbout());
			
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
