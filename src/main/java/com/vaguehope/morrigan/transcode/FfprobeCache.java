package com.vaguehope.morrigan.transcode;

import java.io.File;
import java.io.IOException;
import com.vaguehope.morrigan.util.LruCache;

public class FfprobeCache {

	public static FfprobeCache INSTANCE = new FfprobeCache();

	private final LruCache<String, FfprobeInfo> cache = new LruCache<>(10000);

	private FfprobeCache() {}

	/**
	 * Will not return null.
	 */
	public FfprobeInfo inspect (final File file) throws IOException {
		final String key = file.getAbsolutePath();
		FfprobeInfo info;
		synchronized (this.cache) {
			info = this.cache.get(key);
		}
		if (info != null && info.getFileLastModified() == file.lastModified()) {
			return info;
		}

		info = Ffprobe.inspect(file);

		synchronized (this.cache) {
			this.cache.put(key, info);
		}
		return info;
	}

}
