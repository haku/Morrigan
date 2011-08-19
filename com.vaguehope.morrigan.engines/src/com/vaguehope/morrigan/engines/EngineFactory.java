package com.vaguehope.morrigan.engines;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.engines.common.ImplException;
import com.vaguehope.morrigan.engines.hotkey.HotkeyEngineRegister;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyEngine;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineRegister;
import com.vaguehope.morrigan.model.exceptions.MorriganException;


public class EngineFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final Logger logger = Logger.getLogger(EngineFactory.class.getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static URLClassLoader getClassLoader () throws MorriganException, MalformedURLException {
		File[] files = Config.getPluginJars();
		
		URL jarUrls[] = new URL [files.length];
		for (int i = 0; i < files.length; i++) {
			jarUrls[i] = new URL("jar", "", "file:" + files[i].getAbsolutePath() + "!/");
			logger.fine("loaded jarUrl=" + files[i].getAbsolutePath());
		}
		
		URLClassLoader classLoader = URLClassLoader.newInstance(jarUrls, IPlaybackEngine.class.getClassLoader());
		
		return classLoader;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static IPlaybackEngine makePlaybackEngine () throws ImplException {
		IPlaybackEngine engine = PlaybackEngineRegister.getNewPlaybackEngine();
		
		if (engine == null) {
			logger.warning("Failed to find playback engine using new method, reverting to old.");
    		try {
    			String engineClass = Config.getPlaybackEngineClass();
    			if (engineClass != null) {
        			Class<?> c = getClassLoader().loadClass(engineClass);
        			engine = (IPlaybackEngine) c.newInstance();
        			engine.setClassPath(Config.getPluginJarPaths());
    			}
    		}
    		catch (Exception e) {
    			throw new ImplException(e);
    		}
		}
		
		if (engine != null) {
			logger.info("About " + engine.getClass().getName() + ":\n" + engine.getAbout());
		}
		else {
			logger.warning("Failed to create PlaybackEngine instance.");
		}
		
		return engine;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static boolean canMakeHotkeyEngine () {
		try {
			return (HotkeyEngineRegister.countFactories() > 0 || Config.getHotKeyEngineClass() != null);
		}
		catch (MorriganException e) {
			return false;
		}
	}
	
	public static IHotkeyEngine makeHotkeyEngine () throws ImplException {
		IHotkeyEngine engine = HotkeyEngineRegister.getNewHeykeyEngine();
		
		if (engine == null) {
    		logger.warning("Failed to find hotkey engine using new method, reverting to old.");
    		try {
    			String engineClass = Config.getHotKeyEngineClass();
    			if (engineClass != null) {
        			Class<?> c = getClassLoader().loadClass(engineClass);
        			engine = (IHotkeyEngine) c.newInstance();
        			engine.setClassPath(Config.getPluginJarPaths());
    			}
    		}
    		catch (Exception e) {
    			throw new ImplException(e);
    		}
		}
		
		if (engine != null) {
			logger.info("About " + engine.getClass().getName() + ":\n" + engine.getAbout());
		}
		else {
			logger.warning("Failed to create HotkeyEngine instance.");
		}
		
		return engine;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
