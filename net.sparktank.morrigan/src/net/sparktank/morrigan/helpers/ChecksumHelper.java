package net.sparktank.morrigan.helpers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class ChecksumHelper {
	
	static public long generateCrc32Checksum (String filepath) throws IOException {
		FileInputStream fis;
		BufferedInputStream bis;
		CheckedInputStream cis;
		
		fis = new FileInputStream(filepath);
		try {
			bis = new BufferedInputStream(fis);
			try {
				cis = new CheckedInputStream(bis, new CRC32());
				try {
					while (cis.read() != -1) {}
				} finally {
					cis.close();
				}
			} finally {
				bis.close();
			}
		} finally {
			fis.close();
		}
		
		return cis.getChecksum().getValue();
	}
	
}
