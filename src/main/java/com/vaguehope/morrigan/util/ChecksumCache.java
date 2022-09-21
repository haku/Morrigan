package com.vaguehope.morrigan.util;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

public class ChecksumCache {

	private static final LruCache<String, BigInteger> CACHE = new LruCache<>(10000);

	private static final ThreadLocal<ByteBuffer> BUFFER_FACTRY = new ThreadLocal<ByteBuffer>(){
		@Override
		protected ByteBuffer initialValue () {
			final ByteBuffer b = ChecksumHelper.createByteBuffer();
			b.clear();
			return b;
		}
	};

	public static BigInteger readMd5 (final File file) throws IOException {
		final String key = file.getAbsolutePath();
		BigInteger md5;
		synchronized (CACHE) {
			md5 = CACHE.get(key);
		}
		if (md5 != null) return md5;

		final File cacheFile = new File(file.getAbsolutePath() + ".md5");
		if (md5 == null) {
			if (cacheFile.exists() && cacheFile.lastModified() > file.lastModified()) {
				md5 = new BigInteger(IoHelper.readAsString(cacheFile), 16);
			}
		}

		if (md5 == null) {
			md5 = ChecksumHelper.md5(file, BUFFER_FACTRY.get());
			IoHelper.write(md5.toString(16), cacheFile);
		}

		synchronized (CACHE) {
			CACHE.put(key, md5);
		}
		return md5;
	}

}
