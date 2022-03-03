package com.vaguehope.morrigan.util;

import java.util.Locale;

public class ExceptionHelper {

	public static boolean causedBy (final Throwable throwable, final Class<? extends Throwable> cls) {
		Throwable t = throwable;
		while (true) {
			if (cls.isAssignableFrom(t.getClass())) return true;
			if (t.getCause() == null) return false;
			t = t.getCause();
		}
	}

	public static boolean causedBy (final Throwable throwable, final Class<? extends Throwable> cls, final String msgSubStr) {
		final String msgSubStrLower = msgSubStr.toLowerCase(Locale.ENGLISH);
		Throwable t = throwable;
		while (true) {
			if (cls.isAssignableFrom(t.getClass())
					&& t.getMessage() != null
					&& t.getMessage().toLowerCase(Locale.ENGLISH).contains(msgSubStrLower)) return true;
			if (t.getCause() == null) return false;
			t = t.getCause();
		}
	}

	public static String causeTrace(final Throwable t, final String sep) {
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		Throwable c = t;
		while (c != null) {
			if (!first) sb.append(sep).append("caused by: ");
			sb.append(String.valueOf(c));
			c = c.getCause();
			first = false;
		}
		return sb.toString();
	}

}
