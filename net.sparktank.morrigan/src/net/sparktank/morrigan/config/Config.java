package net.sparktank.morrigan.config;

import java.io.File;

public class Config {
	
	private static final String PL_DIR = "/.morrigan/pl";
	public static final String PL_FILE_EXT = ".morrigan_pl";
	
	public static File getPlDir () {
		File f = new File(System.getProperty("user.home") + PL_DIR);
		if (!f.exists()) f.mkdirs();
		return f;
	}
}
