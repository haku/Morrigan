package net.sparktank.morrigan.config;

import java.io.File;

public class Config {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String DIR_CONFIG = "/.morrigan";
	
	public static String getConfigDir () {
		return System.getProperty("user.home") + DIR_CONFIG;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String PL_DIR = "/pl";
	public static final String PL_FILE_EXT = ".morrigan_pl";
	
	public static File getPlDir () {
		File f = new File(getConfigDir() + PL_DIR);
		if (!f.exists()) f.mkdirs();
		return f;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String LIB_DIR = "/libs";
	public static final String LIB_FILE_EXT = ".db3";
	
	public static File getLibDir () {
		File f = new File(getConfigDir() + LIB_DIR);
		if (!f.exists()) f.mkdirs();
		return f;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/*
	 * TODO move these vars to a user-config somewhere.
	 */
	
	public static final String PLAYBACK_ENGINE_JAR = "D:/haku/development/eclipseWorkspace-java/net.sparktank.morrigan.playbackimpl.spi/morrigan.playbackimpl.spi.jar";
	public static final String PLAYBACK_ENGINE = "net.sparktank.morrigan.playbackimpl.spi.PlaybackEngine";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
