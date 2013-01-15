package com.vaguehope.morrigan.util;

public final class TimeHelper {

	private TimeHelper () {}

	@SuppressWarnings("boxing")
	public static String formatTimeSeconds (long seconds) {
		if (seconds >= 3600) {
			return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
		}
		return String.format("%d:%02d", (seconds % 3600) / 60, (seconds % 60));
	}

	public static String formatTimeMiliseconds (long miliseconds) {
		return String.valueOf(miliseconds / 1000f);
	}

}
