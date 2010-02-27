package net.sparktank.morrigan.engines;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.exceptions.MorriganException;

public class EngineFactory {
	
private static URLClassLoader classLoader = null;
	
	private static URLClassLoader getClassLoader () throws MorriganException, MalformedURLException {
		if (classLoader == null) {
			File[] files = Config.getPluginJars();
			
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
			
			playbackEngine.setClassPath(Config.getPluginJarPaths());
			System.out.println("About " + playbackEngineClass + ":\n" + playbackEngine.getAbout());
			
			return playbackEngine;
			
		} catch (Exception e) {
			throw new ImplException(e);
		}
	}
	
}
