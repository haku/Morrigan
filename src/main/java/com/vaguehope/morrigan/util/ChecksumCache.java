package com.vaguehope.morrigan.util;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

public class ChecksumCache {

	private static final LruCache<String, BigInteger> CACHE = new LruCache<String, BigInteger>(10000);

	private static final ThreadLocal<ByteBuffer> BUFFER_FACTRY = new ThreadLocal<ByteBuffer>(){
		@Override
		protected ByteBuffer initialValue () {
			final ByteBuffer b = ChecksumHelper.createByteBuffer();
			b.clear();
			return b;
		}
	};

	public static BigInteger readHash (final File file) throws IOException {
		final String key = file.getAbsolutePath();
		BigInteger hash;
		synchronized (CACHE) {
			hash = CACHE.get(key);
		}
		if (hash != null) return hash;

		final File cacheFile = new File(file.getAbsolutePath() + ".md5");
		if (hash == null) {
			if (cacheFile.exists() && cacheFile.lastModified() > file.lastModified()) {
				hash = new BigInteger(IoHelper.readAsString(cacheFile), 16);
			}
		}

		if (hash == null) {
			hash = ChecksumHelper.generateMd5(file, BUFFER_FACTRY.get());
			IoHelper.write(hash.toString(16), cacheFile);
		}

		synchronized (CACHE) {
			CACHE.put(key, hash);
		}
		return hash;
	}

}
