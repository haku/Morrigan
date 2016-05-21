package com.vaguehope.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.util.LruCache;

/**
 * TODO cron clean old token files.
 */
public class AuthMgr {

	private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
	private static final long DEFAULT_TOKEN_MAX_FRESH_SECONDS = TimeUnit.DAYS.toSeconds(1);

	private final File sessionDir;
	private final long maxTokenAgeSeconds;
	private final long maxTokenFreshSeconds;

	private final Map<String, Long> cache = Collections.synchronizedMap(new LruCache<String, Long>(100));

	enum TokenValidity {
		INVALID, FRESH, REFRESHED
	}

	public AuthMgr (final int maxTokenAgeSeconds, final ScheduledExecutorService schEs) {
		this.sessionDir = Config.getSessionDir();
		this.maxTokenAgeSeconds = maxTokenAgeSeconds;
		this.maxTokenFreshSeconds = maxTokenAgeSeconds < DEFAULT_TOKEN_MAX_FRESH_SECONDS ? maxTokenAgeSeconds : DEFAULT_TOKEN_MAX_FRESH_SECONDS;

		schEs.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run () {
				try {
					cleanUpTokens();
				}
				catch (final Exception e) {
					e.printStackTrace(); // FIXME
				}
			}
		}, 0, 1, TimeUnit.DAYS);
	}

	public void cleanUpTokens () {
		final File[] files = this.sessionDir.listFiles();
		if (files == null) return;
		final long nowMillis = System.currentTimeMillis();
		for (final File file : files) {
			final long ageSeconds = TimeUnit.MILLISECONDS.toSeconds(nowMillis - file.lastModified());
			if (ageSeconds > this.maxTokenAgeSeconds) file.delete();
		}
	}

	public String newToken () throws IOException {
		final String token = UUID.randomUUID().toString();
		new File(this.sessionDir, token).createNewFile();
		return token;
	}

	public TokenValidity isValidToken (final String token) throws IOException {
		// Valid format?
		if (token == null) return TokenValidity.INVALID;
		if (!UUID_PATTERN.matcher(token).matches()) return TokenValidity.INVALID;

		final long nowMillis = System.currentTimeMillis();

		// In cache and fresh?
		final Long cachedLastModifiedMillis = this.cache.get(token);
		if (cachedLastModifiedMillis != null) {
			final long ageSeconds = TimeUnit.MILLISECONDS.toSeconds(nowMillis - cachedLastModifiedMillis);
			if (ageSeconds < this.maxTokenFreshSeconds) return TokenValidity.FRESH;
			this.cache.remove(token);
		}

		// Exists on disc?
		final File file = new File(this.sessionDir, token);
		if (!file.exists()) return TokenValidity.INVALID;

		// Valid?
		final long lastModifiedMillis = file.lastModified();
		final long ageSeconds = TimeUnit.MILLISECONDS.toSeconds(nowMillis - lastModifiedMillis);
		if (ageSeconds > this.maxTokenAgeSeconds) return TokenValidity.INVALID;

		// Refresh?
		if (ageSeconds > this.maxTokenFreshSeconds) {
			if (!file.setLastModified(nowMillis)) throw new IOException("Failed to update token file timestamp: " + file);
			this.cache.put(token, nowMillis);
			return TokenValidity.REFRESHED;
		}

		this.cache.put(token, lastModifiedMillis);
		return TokenValidity.FRESH;
	}

}
