package net.sparktank.morrigan.config;

import java.io.File;

public class Config {
	
	private static final String PL_DIR = "/.morrigan/pl";
	public static final String PL_FILE_EXT = ".morrigan_pl";
	
	public static File getPlDir () {
		// FIXME what if dir does not exist?
		
		File f = new File(System.getProperty("user.home") + PL_DIR);
		System.out.println("getPlDir absolute path = " + f.getAbsolutePath());
		return f;
	}
}
