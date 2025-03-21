package com.vaguehope.morrigan.server.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public final class XmlHelper {

	private XmlHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static ThreadLocal<SimpleDateFormat> Iso8601Utc = new ThreadLocal<>() {
		@Override
		protected SimpleDateFormat initialValue() {
			SimpleDateFormat a = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			a.setTimeZone(TimeZone.getTimeZone("UTC"));
			return a;
		}
	};

	public static DateFormat getIso8601UtcDateFormatter () {
		return Iso8601Utc.get();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
