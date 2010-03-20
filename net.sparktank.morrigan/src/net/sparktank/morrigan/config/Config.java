package net.sparktank.morrigan.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.FileExtFilter;

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
	public static final String LIB_FILE_EXT = ".db3";
	
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
	
	private static final String PROP_FILE = "morrigan.properties";
	
	private static final String PROP_ENG_PE_CLASS = "engines.playback.class";
	private static final String PROP_ENG_HK_CLASS = "engines.hotkey.class";
	private static final String PROP_ENG_JARDIRS = "engines.jardirs";
	
	private static final String PROP_MEDIA_TYPES = "media.types";
	
	private static Properties properties = null;
	
	private static Properties getProperties () throws MorriganException {
		if (properties==null) {
			File file = new File(PROP_FILE);
			Properties props = new Properties();
			try {
				FileInputStream fis = new FileInputStream(file);
				props.load(fis);
				fis.close();
			} catch (Exception e) {
				throw new MorriganException(e);
			}
			properties = props;
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
		
		return ret.toArray( new File[] {} );
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
		
		return ret.toArray( new File[] {} );
	}
	
	public static String getPlaybackEngineClass () throws MorriganException {
		return getProperties().getProperty(PROP_ENG_PE_CLASS);
	}
	
	public static String getHotKeyEngineClass () throws MorriganException {
		return getProperties().getProperty(PROP_ENG_HK_CLASS);
	}
	
	/**
	 * @return Array of lower-case strings without dots.  e.g. "mp3", "ogg".
	 * @throws ImplException 
	 */
	public static String[] getMediaFileTypes () throws MorriganException {
		String types = getProperties().getProperty(PROP_MEDIA_TYPES);
		String[] arrTypes = types.split("\\|");
		
		for (int i = 0; i < arrTypes.length; i++) {
			arrTypes[i] = arrTypes[i].toLowerCase();
		}
		
		return arrTypes;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
