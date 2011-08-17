package com.vaguehope.morrigan.android.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final int BUFFERSIZE = 8192;
	
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
}
