package com.vaguehope.morrigan.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public final class ErrorHelper {

	private ErrorHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static String getStackTrace (final Throwable t) {
		final Writer writer = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(writer);
		t.printStackTrace(printWriter);
		return writer.toString();
	}

	public static String getCauseTrace (final Throwable t) {
		return causeTrace(t, "\n\ncaused by:\n   ", false);
	}

	public static String oneLineCauseTrace (final Throwable t) {
		return causeTrace(t, " > ", true);
	}

	private static String causeTrace (final Throwable t, final String joiner, final boolean includeLineNumbers) {
		if (t == null) return "Unable to display error message as Throwable object is null.";

		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		Throwable c = t;
		while (c != null) {
			if (!first) sb.append(joiner);
			sb.append(String.valueOf(c));

			if (includeLineNumbers) {
				final StackTraceElement[] st = c.getStackTrace();
				if (st != null && st.length > 0) {
					final StackTraceElement e = st[0];
					sb.append(" (").append(e.getFileName()).append(":").append(e.getLineNumber()).append(")");
				}
			}

			c = c.getCause();
			first = false;
		}
		return sb.toString();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
