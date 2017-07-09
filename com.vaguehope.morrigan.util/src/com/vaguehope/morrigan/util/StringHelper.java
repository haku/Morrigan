package com.vaguehope.morrigan.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

public class StringHelper {

	private StringHelper () { /* UNUSED */}

	public static <T> String join (final T[] c, final String delim) {
		return join(Arrays.asList(c), delim);
	}

	public static <T> String join (final Iterable<T> c, final String delim) {
		final StringBuilder sb = new StringBuilder();

		for (final T i : c) {
			sb.append(i.toString());
			sb.append(delim);
		}
		sb.delete(sb.length() - delim.length(), sb.length());

		return sb.toString();
	}

	@Deprecated
	public static <T> String joinCollection (final Collection<T> collection, final String delim) {
		return join(collection, delim);
	}

	private static final Pattern END_QUOTES = Pattern.compile("^['\"]+|['\"]+$");

	public static String removeEndQuotes (final String s) {
		return END_QUOTES.matcher(s).replaceAll("");
	}

	public static String trimToNull (final String s) {
		if (s == null) return null;
		final String ts = s.trim();
		return ts.length() > 0 ? ts : null;
	}

	/**
	 * Will not return null.
	 */
	public static String trimToEmpty (final String s) {
		if (s == null) return "";
		return s.trim();
	}

	public static String downcase(final String s) {
		if (s == null) return null;
		return s.toLowerCase(Locale.ENGLISH);
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

	public static boolean sameButDifferentCase (final String a, final String b) {
		if (Objs.equals(a, b)) return false;
		return a != null && a.equalsIgnoreCase(b);
	}

	public static boolean startsWithIgnoreCase (final String s, final String start) {
		if (s == null) return start == null;
		return s.toLowerCase(Locale.ENGLISH).startsWith(start.toLowerCase(Locale.ENGLISH));
	}

	public static boolean endsWithIgnoreCase (final String s, final String end) {
		if (s == null) return end == null;
		return s.toLowerCase(Locale.ENGLISH).endsWith(end.toLowerCase(Locale.ENGLISH));
	}

	/**
	 * Returns 2 element array or null.
	 */
	public static String[] splitOnce (final String s, final char sep) {
		final int x = s.indexOf(sep);
		if (x < 0) return null;
		return new String[] { s.substring(0, x), s.substring(x + 1) };
	}

}
