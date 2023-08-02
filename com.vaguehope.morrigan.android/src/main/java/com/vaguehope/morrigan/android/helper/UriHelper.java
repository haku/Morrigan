package com.vaguehope.morrigan.android.helper;

import java.util.List;

import android.net.Uri;

public class UriHelper {

	public static String ensureNoLeadingSlash(final String uri) {
		if (!uri.startsWith("/")) return uri;
		return uri.substring(1);
	}

	public static String ensureNoTrailingSlash(final String uri) {
		if (!uri.endsWith("/")) return uri;
		return uri.substring(0, uri.length() - 1);
	}

	public static String joinParts(final String... parts) {
		final StringBuilder ret = new StringBuilder();
		for (final String part : parts) {
			if (ret.length() > 0) ret.append("/");
			ret.append(ensureNoLeadingSlash(ensureNoTrailingSlash(part)));
		}
		return ret.toString();
	}

	public static String getFileName (final Uri item) {
		if (item == null) return "null";

		final List<String> parts = item.getPathSegments();
		if (parts.size() < 1) return "[empty path]";

		return parts.get(parts.size() - 1);
	}

}
