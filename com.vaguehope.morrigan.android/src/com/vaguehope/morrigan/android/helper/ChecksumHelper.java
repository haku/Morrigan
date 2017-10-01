package com.vaguehope.morrigan.android.helper;

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

	private static final int BUFFERSIZE = 1024 * 64; // 64kb.

	private ChecksumHelper () {}

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

	public static byte[] createBuffer () {
		return new byte[BUFFERSIZE];
	}

	public static BigInteger generateMd5Checksum (final InputStream is, final byte[] buffer) throws IOException {
		final MessageDigest md = MD_MD5_FACTORY.get();
		int n;
		do {
			n = is.read(buffer);
			if (n > 0) md.update(buffer, 0, n);
		}
		while (n != -1);
		return new BigInteger(1, md.digest());
	}

	public static ByteBuffer createByteBuffer () {
		return ByteBuffer.allocateDirect(BUFFERSIZE);
	}

	public static BigInteger generateMd5Checksum (final File file, final ByteBuffer buffer) throws IOException {
		final MessageDigest md = MD_MD5_FACTORY.get();
		final FileInputStream is = new FileInputStream(file);
		try {
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

}
