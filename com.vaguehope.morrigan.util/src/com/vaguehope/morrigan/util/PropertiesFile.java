package com.vaguehope.morrigan.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class PropertiesFile {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String filepath;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PropertiesFile (String filepath) {
		if (filepath == null || filepath.isEmpty()) throw new IllegalArgumentException("Filepath must be set.");
		this.filepath = filepath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getString (String key, String defaultValue) throws IOException {
		String string = getProperties().getProperty(key);
		if (string == null) return defaultValue;
		return string;
	}
	
	public int getInt (String key, int defaultValue) throws IOException {
		String string = getProperties().getProperty(key);
		if (string == null) return defaultValue;
		try {
			return Integer.parseInt(string);
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	public Set<Entry<Object, Object>> getAll () throws IOException {
		return getProperties().entrySet();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Properties propCache = null;
	private Object[] propCacheLock = new Object[0];
	
	private Properties getProperties () throws IOException {
		synchronized (this.propCacheLock) {
			if (this.propCache == null) {
				File file = new File(this.filepath);
				Properties props = new Properties();
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
					props.load(fis);
				}
				catch (FileNotFoundException e) {
					return new Properties(); // No file = empty properties.
				}
				finally {
					if (fis != null) fis.close();
				}
				this.propCache = props;
			}
		}
		return this.propCache;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
