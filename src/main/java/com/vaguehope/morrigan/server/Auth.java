package com.vaguehope.morrigan.server;

import java.util.concurrent.TimeUnit;

public final class Auth {

	private Auth () {}

	static final long MAX_TOKEN_AGE_MILLIS = TimeUnit.DAYS.toMillis(30);
	static final long MAX_TOKEN_FRESH_MILLIS = TimeUnit.DAYS.toMillis(5);
	static final long REFRESH_LOCK_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);
	static final long MAX_EDEN_AGE_MILLIS = TimeUnit.MINUTES.toMillis(5);
	static final String TOKEN_COOKIE_NAME = "MORRIGANTOKEN";

}
