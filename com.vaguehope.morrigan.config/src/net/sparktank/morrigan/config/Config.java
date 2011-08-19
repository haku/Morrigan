package net.sparktank.morrigan.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.vaguehope.morrigan.model.exceptions.MorriganException;


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
	
	private static final String PROP_ENG_PE_CLASS = "engines.playback.class";
	private static final String PROP_ENG_HK_CLASS = "engines.hotkey.class";
	private static final String PROP_ENG_JARDIRS = "engines.jardirs";
	
	private static final String PROP_MEDIA_TYPES = "media.types";
	private static final String PROP_MEDIA_PICTURE_TYPES = "media.pictures.types";
	
	private static Object propertiesLock = new Object();
	private static Properties properties = null;
	
	private static Properties getProperties () throws MorriganException {
		synchronized (propertiesLock) {
			if (properties == null) {
				File file = new File(PROP_FILE);
				System.out.println("PROP_FILE=" + file.getAbsolutePath());
				Properties props = new Properties();
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
					props.load(fis);
				} catch (Exception e) {
					throw new MorriganException(e);
				} finally {
					try {
						if (fis!=null) fis.close();
					} catch (IOException e) {
						throw new MorriganException(e);
					}
				}
				properties = props;
			}
		}
		return properties;
	}
	
	public static File[] getPluginJarPaths () throws MorriganException {
		List<File> ret = new ArrayList<File>();
		
		String data = getProperties().getProperty(PROP_ENG_JARDIRS);
		String[] dirs = data.split("\\|");
		for (String dir : dirs) {
			File dirFile = new File(dir);
			ret.add(dirFile);
		}
		
		File[] files = new File[] {};
		return ret.toArray( files );
	}
	
	/**
	 * Returns list of file objects.
	 * @return
	 * @throws MorriganException
	 */
	public static File[] getPluginJars () throws MorriganException {
		List<File> ret = new ArrayList<File>();
		File[] paths = getPluginJarPaths();
		
		for (File dir : paths) {
			File[] listFiles = dir.listFiles(new FileExtFilter("jar"));
			if (listFiles!=null && listFiles.length>0) {
				ret.addAll(Arrays.asList(listFiles));
			}
		}
		
		File[] files = new File[] {};
		return ret.toArray( files );
	}
	
	public static String getPlaybackEngineClass () throws MorriganException {
		return getProperties().getProperty(PROP_ENG_PE_CLASS);
	}
	
	public static String getHotKeyEngineClass () throws MorriganException {
		return getProperties().getProperty(PROP_ENG_HK_CLASS);
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
	
	static private String WAR_FILE = "morrigan.wui.war";
	static private String WAR_PROP = "wui.war";
	
	static public String getWuiWarLocation () throws MorriganException {
		File f = new File(WAR_FILE);
		if (f.exists()) return f.getAbsolutePath();
		
		String p = getProperties().getProperty(WAR_PROP);
		if (p != null) return p;
		
		throw new MorriganException("Failed to find " + WAR_FILE);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
