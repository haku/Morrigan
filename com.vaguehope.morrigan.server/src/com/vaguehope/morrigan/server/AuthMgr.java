package com.vaguehope.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.util.LruCache;

public class AuthMgr {

	private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

	private final File sessionDir;

	private final Map<String, Long> cache = Collections.synchronizedMap(new LruCache<String, Long>(100));
	private final ConcurrentMap<String, Long> refreshed = new ConcurrentHashMap<String, Long>();

	enum TokenValidity {
		INVALID, FRESH, REFRESH_REQUEST
	}

	public AuthMgr (final ScheduledExecutorService schEs) {
		this.sessionDir = Config.getSessionDir();
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
		final long nowMillis = System.currentTimeMillis();

		final File[] files = this.sessionDir.listFiles();
		if (files != null) {
			for (final File file : files) {
				if (nowMillis - file.lastModified() > Auth.MAX_TOKEN_AGE_MILLIS) file.delete();
			}
		}

		for (final Entry<String, Long> entry : this.refreshed.entrySet()) {
			if (nowMillis - entry.getValue() > Auth.REFRESH_LOCK_INTERVAL_MILLIS) {
				this.refreshed.remove(entry.getKey(), entry.getValue());
			}
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

		// In cache?
		final Long cachedLastModifiedMillis = this.cache.get(token);
		if (cachedLastModifiedMillis != null) {
			return isTokenAgeValid(token, nowMillis - cachedLastModifiedMillis, nowMillis);
		}

		// Exists on disc?
		final File file = new File(this.sessionDir, token);
		if (!file.exists()) return TokenValidity.INVALID;

		// Read from disc.
		final long lastModifiedMillis = file.lastModified();
		this.cache.put(token, lastModifiedMillis);

		return isTokenAgeValid(token, nowMillis - lastModifiedMillis, nowMillis);
	}

	private TokenValidity isTokenAgeValid (final String token, final long ageMillis, final long nowMillis) {
		if (ageMillis > Auth.MAX_TOKEN_AGE_MILLIS) return TokenValidity.INVALID;

		if (ageMillis > Auth.MAX_TOKEN_FRESH_MILLIS) {
			final Long lastRefreshedMillis = this.refreshed.get(token);
			if (lastRefreshedMillis == null) {
				if (this.refreshed.putIfAbsent(token, nowMillis) == null) return TokenValidity.REFRESH_REQUEST;
			}
			else if (nowMillis - lastRefreshedMillis > Auth.REFRESH_LOCK_INTERVAL_MILLIS) {
				if (this.refreshed.replace(token, lastRefreshedMillis, nowMillis)) return TokenValidity.REFRESH_REQUEST;
			}
		}

		return TokenValidity.FRESH;
	}

}
