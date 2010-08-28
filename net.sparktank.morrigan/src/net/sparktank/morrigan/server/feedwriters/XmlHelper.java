package net.sparktank.morrigan.server.feedwriters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class XmlHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private ThreadLocal<SimpleDateFormat> Iso8601Utc = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			SimpleDateFormat a = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			a.setTimeZone(TimeZone.getTimeZone("UTC"));
			return a;
		}
	};
	
	static public DateFormat getIso8601UtcDateFormatter () {
		return Iso8601Utc.get();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
