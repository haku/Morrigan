package net.sparktank.morrigan.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void copyFile(File sourceFile, File destFile) throws IOException {
		copyFile(sourceFile, destFile, false);
	}
	
	static public void copyFile(File sourceFile, File destFile, boolean overWrite) throws IOException {
		if (destFile.exists()) {
			if (!overWrite) {
				throw new IllegalArgumentException("Target file already exists.");
			}
		} else {
			destFile.createNewFile();
		}
		
		FileChannel source = null;
		FileChannel destination = null;
		
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
