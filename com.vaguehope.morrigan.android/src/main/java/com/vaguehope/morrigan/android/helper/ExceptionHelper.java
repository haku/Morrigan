package com.vaguehope.morrigan.android.helper;

public final class ExceptionHelper {

	private ExceptionHelper () {
		throw new AssertionError();
	}

	/**
	 * Causes separated by new lines.
	 */
	public static String causeTrace (final Throwable t) {
		return causeTrace(t, "\n");
	}

	public static String causeTrace (final Throwable t, final String sep) {
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

	public static String veryShortMessage(final Throwable t) {
		String msg = t.getMessage();
		if (!StringHelper.isEmpty(msg)) return msg;
		msg = t.getClass().getSimpleName();
		if (!StringHelper.isEmpty(msg)) return msg;
		return String.valueOf(t);
	}

}
