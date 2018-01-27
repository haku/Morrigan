package com.vaguehope.morrigan.android.helper;

import java.util.List;

import android.net.Uri;

public class UriHelper {

	public static String getFileName (final Uri item) {
		if (item == null) return "null";

		final List<String> parts = item.getPathSegments();
		if (parts.size() < 1) return "[empty path]";

		return parts.get(parts.size() - 1);
	}

}
