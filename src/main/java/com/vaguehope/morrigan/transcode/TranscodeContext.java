package com.vaguehope.morrigan.transcode;

import com.vaguehope.morrigan.config.Config;

public class TranscodeContext {

	public final Config config;
	public final FfprobeCache ffprobeCache;

	public TranscodeContext(final Config config) {
		this(config, FfprobeCache.INSTANCE);
	}

	public TranscodeContext(final Config config, final FfprobeCache ffprobeCache) {
		this.config = config;
		this.ffprobeCache = ffprobeCache;
	}

}
