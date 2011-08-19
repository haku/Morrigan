package com.vaguehope.morrigan.util;

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
	
	static public void copyFile(File srcFile, File dstFile, boolean overWrite) throws IOException {
		if (dstFile.exists()) {
			if (!overWrite) {
				throw new IllegalArgumentException("Target file already exists.");
			}
		} else {
			dstFile.createNewFile();
		}
		
		FileChannel srcChannel = null;
		FileChannel dstChannel = null;
		
		try {
			boolean transferDone = false;
			int chunkSizeMb = 64;
			
			while (!transferDone) {
				srcChannel = new FileInputStream(srcFile).getChannel();
				dstChannel = new FileOutputStream(dstFile).getChannel();
				
				try {
					int maxCount = (chunkSizeMb * 1024 * 1024) - (32 * 1024);
					long size = srcChannel.size();
					long position = 0;
					while (position < size) {
						position += srcChannel.transferTo(position, maxCount, dstChannel);
					}
					transferDone = true;
				}
				catch (IOException e) {
					if (e.getMessage().contains("Insufficient system resources exist to complete the requested service")) {
						chunkSizeMb--;
						if (chunkSizeMb <= 0) {
							throw e; // We have run out of options.
						}
						System.err.println("Reduced chunk size to " + chunkSizeMb + " mb.");
						srcChannel.close();
						if (dstChannel != null) dstChannel.close();
					}
					else {
						throw e;
					}
					
				}
			}
		}
		finally {
			if (srcChannel != null) {
				srcChannel.close();
			}
			if (dstChannel != null) {
				dstChannel.close();
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
