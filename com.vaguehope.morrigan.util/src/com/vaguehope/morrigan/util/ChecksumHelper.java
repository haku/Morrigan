package com.vaguehope.morrigan.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ChecksumHelper {

	private ChecksumHelper () {}

	/**
	 * MessageDigest.getInstance("MD5") can take up to a second,
	 * so using this to cache it and improve performance.
	 * Not sure if MessageDigest is thread-safe, so using ThreadLocal
	 * just in case.
	 */
	private static ThreadLocal<MessageDigest> MD_MD5_FACTORY = new ThreadLocal<MessageDigest>() {
		@Override
		protected MessageDigest initialValue () {
			try {
				return MessageDigest.getInstance("MD5");
			}
			catch (final NoSuchAlgorithmException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public MessageDigest get () {
			final MessageDigest md = super.get();
			md.reset();
			return md;
		}
	};

	private static final int BUFFERSIZE = 1024 * 64; // 64kb.

	private static byte[] createBuffer () {
		return new byte[BUFFERSIZE];
	}

	public static BigInteger generateMd5Checksum (final File file) throws IOException {
		return generateMd5Checksum(file, createBuffer());
	}

	public static BigInteger generateMd5Checksum (final File file, final byte[] buffer) throws IOException {
		final InputStream is = new FileInputStream(file);
		try {
			final MessageDigest md = MD_MD5_FACTORY.get();
			int n;
			do {
				n = is.read(buffer);
				if (n > 0) md.update(buffer, 0, n);
			}
			while (n != -1);
			return new BigInteger(1, md.digest());
		}
		finally {
			IoHelper.closeQuietly(is);
		}
	}

	public static ByteBuffer createByteBuffer () {
		return ByteBuffer.allocateDirect(BUFFERSIZE);
	}

	public static BigInteger generateMd5Checksum (final File file, final ByteBuffer buffer) throws IOException {
		final FileInputStream is = new FileInputStream(file);
		try {
			final MessageDigest md = MD_MD5_FACTORY.get();
			final FileChannel fc = is.getChannel();
			while (fc.position() < fc.size()) {
				buffer.clear();
				fc.read(buffer);
				buffer.flip();
				md.update(buffer);
			}
			return new BigInteger(1, md.digest());
		}
		finally {
			IoHelper.closeQuietly(is);
		}
	}

	public static String md5String (final String text) {
		final MessageDigest md = MD_MD5_FACTORY.get();
		md.update(text.getBytes(), 0, text.length());
		return new BigInteger(1, md.digest()).toString(16);
	}

}
