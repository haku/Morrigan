package com.vaguehope.morrigan.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import com.vaguehope.morrigan.model.exceptions.MorriganException;


public class Config {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final Logger logger = Logger.getLogger(Config.class.getName());
	
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
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new RuntimeException("Failed to create direactory '"+f.getAbsolutePath()+"'.");
			}
		}
		return f;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String LIB_DIR = "/libs";
	public static final String LIB_LOCAL_FILE_EXT = ".local.db3";
	public static final String LIB_REMOTE_FILE_EXT = ".remote.db3";
	
	public static File getLibDir () {
		File f = new File(getConfigDir() + LIB_DIR);
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new RuntimeException("Failed to create direactory '"+f.getAbsolutePath()+"'.");
			}
		}
		return f;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String GALLERY_DIR = "/gals";
	public static final String GALLERY_LOCAL_FILE_EXT = ".local.db3";
	
	public static File getGalleryDir () {
		File f = new File(getConfigDir() + GALLERY_DIR);
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new RuntimeException("Failed to create direactory '"+f.getAbsolutePath()+"'.");
			}
		}
		return f;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String MMDB_DIR = "/mmdb";
	public static final String MMDB_LOCAL_FILE_EXT = ".local.db3";
	public static final String MMDB_REMOTE_FILE_EXT = ".remote.db3";
	
	public static File getMmdbDir () {
		File f = new File(getConfigDir() + MMDB_DIR);
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new RuntimeException("Failed to create direactory '"+f.getAbsolutePath()+"'.");
			}
		}
		return f;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String PROP_FILE = "morrigan.properties";
	
	private static final String PROP_MEDIA_TYPES = "media.types";
	private static final String PROP_MEDIA_PICTURE_TYPES = "media.pictures.types";
	
	private static Object propertiesLock = new Object();
	private static Properties properties = null;
	
	private static Properties getProperties () throws MorriganException {
		synchronized (propertiesLock) {
			if (properties == null) {
				// This is a hack to try and make it work correctly on OSX.
				File file = new File(new File(PROP_FILE).getAbsolutePath());
				logger.info("PROP_FILE=" + file.getAbsolutePath());
				Properties props = new Properties();
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
					props.load(fis);
				}
				catch (IOException e) {
					throw new MorriganException(e);
				}
				finally {
					try {
						if (fis!=null) fis.close();
					}
					catch (IOException e) {
						throw new MorriganException(e);
					}
				}
				properties = props;
			}
		}
		return properties;
	}
	
	/**
	 * @return Array of lower-case strings without dots.  e.g. "mp3", "ogg".
	 */
	public static String[] getMediaFileTypes () throws MorriganException {
		String types = getProperties().getProperty(PROP_MEDIA_TYPES);
		String[] arrTypes = types.split("\\|");
		
		for (int i = 0; i < arrTypes.length; i++) {
			arrTypes[i] = arrTypes[i].toLowerCase();
		}
		
		return arrTypes;
	}
	
	/**
	 * @return Array of lower-case strings without dots.  e.g. "mp3", "ogg".
	 */
	public static String[] getPictureFileTypes () throws MorriganException {
		String types = getProperties().getProperty(PROP_MEDIA_PICTURE_TYPES);
		String[] arrTypes = types.split("\\|");
		
		for (int i = 0; i < arrTypes.length; i++) {
			arrTypes[i] = arrTypes[i].toLowerCase();
		}
		
		return arrTypes;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
