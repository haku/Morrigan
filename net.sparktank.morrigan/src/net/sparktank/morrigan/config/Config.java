package net.sparktank.morrigan.config;

import java.io.File;

public class Config {
	
	private static final String DIR_CONFIG = "/.morrigan";
	
	public static String getConfigDir () {
		return System.getProperty("user.home") + DIR_CONFIG;
	}
	
	private static final String PL_DIR = "/pl";
	public static final String PL_FILE_EXT = ".morrigan_pl";
	
	public static File getPlDir () {
		File f = new File(getConfigDir() + PL_DIR);
		if (!f.exists()) f.mkdirs();
		return f;
	}
	
	public static final String SQLITE_DBNAME_TITLE = "Local Library";
	private static final String SQLITE_DBNAME_FILENAME = "/library.db3";
	
	public static String getLocalDbFile () {
		return getConfigDir() + SQLITE_DBNAME_FILENAME;
	}
	
	public static final String PLAYBACK_ENGINE = "net.sparktank.morrigan.playbackimpl.spi.PlaybackEngine";
	
}
