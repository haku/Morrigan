package com.vaguehope.morrigan.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ChecksumHelper {

	static final int BUFFERSIZE = 1024 * 64; // 64kb.

	private ChecksumHelper() {
	}

	public static ByteBuffer createByteBuffer() {
		return ByteBuffer.allocateDirect(BUFFERSIZE);
	}

	public static BigInteger generateMd5(final File file, final ByteBuffer buffer) throws IOException {
		final MessageDigest md = MD5_FACTORY.get();
		try (final FileInputStream is = new FileInputStream(file)) {
			try (final FileChannel fc = is.getChannel()) {
				while (fc.position() < fc.size()) {
					buffer.clear();
					fc.read(buffer);
					buffer.flip();
					md.update(buffer);
				}
			}
		}
		return new BigInteger(1, md.digest());
	}

	public static class Md5AndSha1 {
		private final BigInteger md5;
		private final BigInteger sha1;

		public Md5AndSha1(final BigInteger md5, final BigInteger sha1) {
			this.md5 = md5;
			this.sha1 = sha1;
		}

		public BigInteger getMd5() {
			return this.md5;
		}

		public BigInteger getSha1() {
			return this.sha1;
		}
	}

	public static Md5AndSha1 generateMd5AndSha1(final File file, final ByteBuffer buffer) throws IOException {
		final MessageDigest md5 = MD5_FACTORY.get();
		final MessageDigest sha1 = SHA1_FACTORY.get();
		multiMd(file, buffer, md5, sha1);
		return new Md5AndSha1(new BigInteger(1, md5.digest()), new BigInteger(1, sha1.digest()));
	}

	private static void multiMd(final File file, final ByteBuffer buffer, final MessageDigest md0, final MessageDigest md1) throws IOException {
		try (final FileInputStream is = new FileInputStream(file)) {
			try (final FileChannel fc = is.getChannel()) {
				while (fc.position() < fc.size()) {
					buffer.clear();
					fc.read(buffer);
					buffer.flip();
					md0.update(buffer);
					buffer.rewind();
					md1.update(buffer);
				}
			}
		}
	}

	public static String md5String(final String text) {
		final MessageDigest md = MD5_FACTORY.get();
		md.update(text.getBytes(), 0, text.length());
		return new BigInteger(1, md.digest()).toString(16);
	}

	private static final MdFactory MD5_FACTORY = new MdFactory("MD5");
	private static final MdFactory SHA1_FACTORY = new MdFactory("SHA1");

	private static class MdFactory extends ThreadLocal<MessageDigest> {
		private final String algorithm;

		public MdFactory(final String algorithm) {
			this.algorithm = algorithm;
		}

		@Override
		protected MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance(this.algorithm);
			}
			catch (final NoSuchAlgorithmException e) {
				throw new IllegalStateException("Algorithm not found: " + this.algorithm, e);
			}
		}

		@Override
		public MessageDigest get() {
			final MessageDigest md = super.get();
			md.reset();
			return md;
		}
	}

}
