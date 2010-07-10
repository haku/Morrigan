package net.sparktank.nemain.config;

import java.io.File;

public class Config {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String DIR_CONFIG = "/.morrigan/nemain";
	
	public static String getConfigDir () {
		return System.getProperty("user.home") + DIR_CONFIG;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String LIB_DIR = "/db";
	public static final String LIB_ABS_FILE_EXT = ".db3";
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
	
	public static String getFullPathToDb (String fileName) {
		File libDir = Config.getLibDir();
		String libFile = libDir.getPath() + File.separator + fileName;
		
		if (!libFile.toLowerCase().endsWith(Config.LIB_FILE_EXT)) {
			libFile = libFile.concat(Config.LIB_FILE_EXT);
		}
		
		return libFile;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
