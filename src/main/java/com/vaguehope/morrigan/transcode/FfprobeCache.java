package com.vaguehope.morrigan.transcode;

import java.io.File;
import java.io.IOException;
import com.vaguehope.morrigan.util.LruCache;

public class FfprobeCache {

	private static final LruCache<String, FfprobeInfo> CACHE = new LruCache<String, FfprobeInfo>(10000);

	/**
	 * Will not return null.
	 */
	public static FfprobeInfo inspect (final File file) throws IOException {
		final String key = file.getAbsolutePath();
		FfprobeInfo info;
		synchronized (CACHE) {
			info = CACHE.get(key);
		}
		if (info != null && info.getFileLastModified() == file.lastModified()) {
			return info;
		}

		info = Ffprobe.inspect(file);

		synchronized (CACHE) {
			CACHE.put(key, info);
		}
		return info;
	}

}
