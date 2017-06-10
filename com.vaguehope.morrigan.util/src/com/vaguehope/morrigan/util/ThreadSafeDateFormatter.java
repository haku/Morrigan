package com.vaguehope.morrigan.util;

import java.text.SimpleDateFormat;

public class ThreadSafeDateFormatter {

	private final FormatTl threadLocal;

	public ThreadSafeDateFormatter (final String format) {
		this.threadLocal = new FormatTl(format);
	}

	public SimpleDateFormat get () {
		return this.threadLocal.get();
	}

	private final class FormatTl extends ThreadLocal<SimpleDateFormat> {

		private final String format;

		public FormatTl (final String format) {
			this.format = format;
		}

		@Override
		protected SimpleDateFormat initialValue () {
			final SimpleDateFormat a = new SimpleDateFormat(this.format);
			//a.setTimeZone(TimeZone.getTimeZone("UTC"));
			return a;
		}
	}

}
