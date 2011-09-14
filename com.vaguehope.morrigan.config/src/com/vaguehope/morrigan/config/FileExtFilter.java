package com.vaguehope.morrigan.config;

import java.io.File;
import java.io.FileFilter;

public class FileExtFilter implements FileFilter {
	
	private final String[] ext;
	
	public FileExtFilter (String... ext) {
		this.ext = ext;
	}
	
	@Override
	public boolean accept (File file) {
		for (String e : this.ext) {
			if (file.getName().toLowerCase().endsWith(e)) {
				return true;
			}
		}
		return false;
	}
	
}
