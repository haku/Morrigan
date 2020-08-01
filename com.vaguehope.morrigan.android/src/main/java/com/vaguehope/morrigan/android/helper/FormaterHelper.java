package com.vaguehope.morrigan.android.helper;

import java.text.DecimalFormat;

public class FormaterHelper {

	private FormaterHelper () {}

	public static String readableFileSize (final long size) {
		// http://stackoverflow.com/questions/3263892/format-file-size-as-mb-gb-etc
		if (size <= 0) return "0";
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		final int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

}
