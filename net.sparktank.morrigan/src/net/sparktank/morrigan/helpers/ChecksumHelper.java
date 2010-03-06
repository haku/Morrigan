package net.sparktank.morrigan.helpers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class ChecksumHelper {
	
	static public long generateCrc32Checksum (String filepath) throws IOException {
		FileInputStream fis = new FileInputStream(filepath);
		BufferedInputStream bis = new BufferedInputStream(fis);
		CheckedInputStream cis = new CheckedInputStream(bis, new CRC32());
		
		while (cis.read() != -1) {}
		cis.close();
		bis.close();
		fis.close();
		
		return cis.getChecksum().getValue();
	}
	
}
