/*
 * Copyright 2010 Fae Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.vaguehope.morrigan.android.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
		return formatTimeSeconds((long) (miliseconds / 1000f));
	}

	private static ThreadLocal<SimpleDateFormat> ISO_8601_UTC = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue () {
			final SimpleDateFormat a = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			a.setTimeZone(TimeZone.getTimeZone("UTC"));
			return a;
		}
	};

	public static long parseXmlDate (final String date) throws ParseException {
		final Date d = ISO_8601_UTC.get().parse(date);
		if (d == null) return 0L;
		return d.getTime();
	}

}
