package com.vaguehope.morrigan.util;

import java.util.Collection;
import java.util.regex.Pattern;

public class StringHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private StringHelper () { /* UNUSED */ }

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static <T> String joinCollection (final Collection<T> collection, final String delim) {
		StringBuilder sb = new StringBuilder();

		for (T i : collection) {
			sb.append(i.toString());
			sb.append(delim);
		}
		sb.delete(sb.length() - delim.length(), sb.length());

		return sb.toString();
	}

	private static final Pattern END_QUOTES = Pattern.compile("^['\"]+|['\"]+$");

	public static String removeEndQuotes (final String s) {
		return END_QUOTES.matcher(s).replaceAll("");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
