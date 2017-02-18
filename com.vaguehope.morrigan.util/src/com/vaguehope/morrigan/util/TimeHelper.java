package com.vaguehope.morrigan.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeHelper {

	private TimeHelper () {}

	@SuppressWarnings("boxing")
	public static String formatTimeSeconds (final long seconds) {
		if (seconds >= 3600) {
			return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
		}
		return String.format("%d:%02d", (seconds % 3600) / 60, (seconds % 60));
	}

	public static String formatTimeMiliseconds (final long miliseconds) {
		return String.valueOf(miliseconds / 1000f);
	}

	private static final Pattern DURATION = Pattern.compile("^(?:(?:([0-9]+):)?([0-9]+):)?([0-9]+)$");

	public static Long parseDuration (final String str) {
		final Matcher m = DURATION.matcher(str.replace(" ", ""));
		if (m.matches()) {
			return (long) parseInt(m.group(1)) * 3600
					+ parseInt(m.group(2)) * 60
					+ parseInt(m.group(3));
		}
		return null;
	}

	private static int parseInt (final String s) {
		if (s == null) return 0;
		return Integer.parseInt(s);
	}

}
