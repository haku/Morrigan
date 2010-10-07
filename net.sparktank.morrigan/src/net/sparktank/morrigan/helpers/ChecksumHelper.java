package net.sparktank.morrigan.helpers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
					while (cis.read() != -1) {/* UNUSED */}
					return cis.getChecksum().getValue();
					
				} finally {
					cis.close();
				}
			} finally {
				bis.close();
			}
		} finally {
			fis.close();
		}
		
	}
	
	public static String Md5String(String text) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		
		byte[] md5hash = new byte[32];
		md.update(text.getBytes(), 0, text.length());
		md5hash = md.digest();
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < md5hash.length; i++) {
			String c = (Integer.toHexString(0xFF & md5hash[i]));
			if (c.length() == 1) sb.append("0");
			sb.append(c);
		}
		
		return sb.toString();
	}
	
}
