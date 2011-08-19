package com.vaguehope.morrigan.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class ChecksumHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
	
	public static final int BUFFERSIZE = 8192;
	
	static public byte[] createBuffer () {
		return new byte[BUFFERSIZE];
	}
	
	static public BigInteger generateMd5Checksum (File file) throws IOException {
		return generateMd5Checksum(file, createBuffer());
	}
	
	static public BigInteger generateMd5Checksum (File file, byte[] buffer) throws IOException {
		MessageDigest md = mdMd5Factory.get();
		md.reset();
		
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			
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
	
	static public ByteBuffer createByteBuffer () {
		return ByteBuffer.allocateDirect(BUFFERSIZE);
	}
	
	static public BigInteger generateMd5Checksum (File file, ByteBuffer buffer) throws IOException {
		MessageDigest md = mdMd5Factory.get();
		md.reset();
		
		FileInputStream is = null;
		try {
			is = new FileInputStream(file);
			FileChannel fc = is.getChannel();
			
			while (fc.position() < fc.size()) {
				buffer.clear();
				fc.read(buffer);
				buffer.flip();
				md.update(buffer);
			}
			
			BigInteger bi = new BigInteger(1, md.digest());
			return bi;
		}
		finally {
			if (is != null) is.close();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public String md5String(String text) {
		MessageDigest md = mdMd5Factory.get();
		md.reset();
		
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
