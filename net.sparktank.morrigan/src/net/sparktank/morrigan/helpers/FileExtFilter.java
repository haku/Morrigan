package net.sparktank.morrigan.helpers;

import java.io.File;
import java.io.FileFilter;

public class FileExtFilter implements FileFilter {
	private final String[] ext;
	
	public FileExtFilter (String... ext) {
		this.ext = ext;
	}
	
	public boolean accept(File file) {
		for (String e : ext) {
			if (file.getName().toLowerCase().endsWith(e)) {
				return true;
			}
		}
		return false;
	}
	
}
