package net.sparktank.morrigan.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
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
	
	/**
	 * MessageDigest.getInstance("MD5") can take up to a second,
	 * so using this to cache it and improve performance.
	 * Not sure if MessageDigest is thread-safe, so using ThreadLocal
	 * just in case.
	 * TODO find out if MessageDigest is thread-safe.
	 */
	static public ThreadLocal<MessageDigest> mdMd5Factory = new ThreadLocal<MessageDigest>() {
		@Override
		protected MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance("MD5");
			}
			catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	static public BigInteger generateMd5Checksum (File file) throws IOException {
		InputStream is = null;
		byte[] buffer = new byte[1024]; // TODO is this the right buffer size to use?
		
		try {
			is = new FileInputStream(file);
			MessageDigest md = mdMd5Factory.get();
			
			int n;
			do {
				n = is.read(buffer);
				if (n > 0) md.update(buffer, 0, n);
			} while (n != -1);
			
			BigInteger bi = new BigInteger(1, md.digest());
			return bi;
		}
		finally {
			if (is != null) is.close();
		}
	}
	
	static public String md5String(String text) {
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
