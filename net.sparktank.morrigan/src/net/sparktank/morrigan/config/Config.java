package net.sparktank.morrigan.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
	
	private static final String PROP_FILE = "morrigan.properties";
	
	private static final String PROP_PE_CLASS = "playbackengine.class";
	private static final String PROP_PE_JARDIRS = "playbackengine.jardirs";
	
	/**
	 * Returns list of file objects.
	 * @return
	 * @throws MorriganException
	 */
	public static File[] getPlaybackEngineJars () throws MorriganException {
		File file = new File(PROP_FILE);
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(file));
		} catch (Exception e) {
			throw new MorriganException(e);
		}
		
		List<File> ret = new ArrayList<File>();
		
		String data = props.getProperty(PROP_PE_JARDIRS);
		String[] dirs = data.split("\\|");
		for (String dir : dirs) {
			File dirFile = new File(dir);
			File[] listFiles = dirFile.listFiles(new FileExtFilter("jar"));
			if (listFiles!=null && listFiles.length>0) {
				ret.addAll(Arrays.asList(listFiles));
			}
		}
		
		return ret.toArray( new File[] {} );
	}
	
	public static String getPlaybackEngineClass () throws MorriganException {
		File file = new File(PROP_FILE);
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(file));
		} catch (Exception e) {
			throw new MorriganException(e);
		}
		
		return props.getProperty(PROP_PE_CLASS);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
