package com.vaguehope.morrigan.android.playback;

import android.net.Uri;

public enum QueueItemType {
	STOP;

	public static final String SCHEME = "mn";

	public Uri toUri () {
		return new Uri.Builder().scheme(SCHEME).opaquePart(name()).build();
	}

	public static QueueItemType parseUri (final Uri uri) {
		final String part = uri.getEncodedSchemeSpecificPart();
		for (final QueueItemType t : values()) {
			if (t.name().equalsIgnoreCase(part)) return t;
		}
		return null;
	}

	public static String parseTitle (final Uri uri) {
		 return uri.getEncodedSchemeSpecificPart();
	}

}
