package com.vaguehope.morrigan.server.feedreader;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.util.httpclient.Http;

public class Auth {

	private static final String USERNAME = "Morrigan";

	public static void addTo (final Map<String, String> headers, final URI uri) {
		addTo(headers, uri, null);
	}

	public static void addTo (final Map<String, String> headers, final URI uri, final String passwd) {
		final String userInfo = uri.getUserInfo();
		if (StringHelper.notBlank(userInfo)) {
			final int x = userInfo.indexOf(':');
			if (x >= 0) {
				final String u = userInfo.substring(0, x);
				final String p = userInfo.substring(x + 1);
				addAuthHeader(headers, u, p);
				return;
			}
			else {
				throw new IllegalArgumentException("URL has invalid user info.");
			}
		}

		if (StringHelper.notBlank(passwd)) addAuthHeader(headers, USERNAME, passwd);
	}

	private static void addAuthHeader (final Map<String, String> headers, final String user, final String pass) {
		final String auth = user + ":" + pass;
		final String enc = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
		headers.put(Http.HEADER_AUTHORISATION, Http.HEADER_AUTHORISATION_PREFIX + enc);
	}

}
