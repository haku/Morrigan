package com.vaguehope.morrigan.dlna.util;

import java.util.Collection;

public final class StringHelper {

	private StringHelper () {
		throw new AssertionError();
	}

	public static String unquoteQuotes (final String in) {
		final String s = in.replace("\\\"", "\"");
		if (s.startsWith("\"")) {
			if (s.endsWith("\"")) return s.substring(1, s.length() - 1);
			return s.substring(1, s.length());
		}
		if (s.endsWith("\"")) return s.substring(0, s.length() - 1);
		return s;
	}

	public static String join (final Collection<?> arr, final String sep) {
		final StringBuilder s = new StringBuilder();
		for (final Object obj : arr) {
			if (s.length() > 0) s.append(sep);
			s.append(obj.toString());
		}
		return s.toString();
	}

	public static boolean blank (final String s) {
		return s == null || s.trim().length() < 1;
	}

	public static boolean notBlank (final String s) {
		return s != null && s.trim().length() > 0;
	}

	public static String removeStart (final String s, final String remove) {
		if (s.startsWith(remove)) return s.substring(remove.length());
		return s;
	}

}
