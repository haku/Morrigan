package net.sparktank.morrigan.engines;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.engines.hotkey.HotkeyEngineRegister;
import net.sparktank.morrigan.engines.hotkey.IHotkeyEngine;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.engines.playback.PlaybackEngineRegister;
import net.sparktank.morrigan.model.exceptions.MorriganException;

public class EngineFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static URLClassLoader getClassLoader () throws MorriganException, MalformedURLException {
		File[] files = Config.getPluginJars();
		
		URL jarUrls[] = new URL [files.length];
		for (int i = 0; i < files.length; i++) {
			jarUrls[i] = new URL("jar", "", "file:" + files[i].getAbsolutePath() + "!/");
			System.err.println("loaded jarUrl=" + files[i].getAbsolutePath());
		}
		
		URLClassLoader classLoader = URLClassLoader.newInstance(jarUrls, IPlaybackEngine.class.getClassLoader());
		
		return classLoader;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static IPlaybackEngine makePlaybackEngine () throws ImplException {
		IPlaybackEngine engine = PlaybackEngineRegister.getNewPlaybackEngine();
		if (engine != null) {
			return engine;
		}
		System.err.println("Failed to find playback engine using new method, reverting to old.");
		
		try {
			String playbackEngineClass = Config.getPlaybackEngineClass();
			Class<?> c = getClassLoader().loadClass(playbackEngineClass);
			IPlaybackEngine playbackEngine = (IPlaybackEngine) c.newInstance();
			
			playbackEngine.setClassPath(Config.getPluginJarPaths());
			System.err.println("About " + playbackEngineClass + ":\n" + playbackEngine.getAbout());
			
			return playbackEngine;
		}
		catch (Exception e) {
			throw new ImplException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static boolean canMakeHotkeyEngine () {
		try {
			return (Config.getHotKeyEngineClass() != null);
		} catch (MorriganException e) {
			return false;
		}
	}
	
	public static IHotkeyEngine makeHotkeyEngine () throws ImplException {
		IHotkeyEngine engine = HotkeyEngineRegister.getNewHeykeyEngine();
		if (engine != null) {
			return engine;
		}
		System.err.println("Failed to find hotkey engine using new method, reverting to old.");
		
		try {
			String engineClass = Config.getHotKeyEngineClass();
			if (engineClass == null) return null;
			
			Class<?> c = getClassLoader().loadClass(engineClass);
			engine = (IHotkeyEngine) c.newInstance();
			
			engine.setClassPath(Config.getPluginJarPaths());
			System.err.println("About " + engineClass + ":\n" + engine.getAbout());
			
			return engine;
		}
		catch (Exception e) {
			throw new ImplException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
