package com.vaguehope.morrigan.android.helper;

public class StringHelper {

	private StringHelper () {
		throw new AssertionError();
	}

	public static boolean isEmpty (final String s) {
		return s == null || s.isEmpty();
	}

	public static String implode (String[] arr, String sep) {
		if (arr == null) return null;
		StringBuilder s = new StringBuilder();
		for (String a : arr) s.append(a).append(sep);
		s.delete(s.length() - sep.length(), s.length());
		return s.toString();
	}

	public static String substringByTokens (String d, String k0, String k1) {
		String ret;
		int x0;
		int l;

		try {
			if (k0 == null) {
				x0 = 0;
				l = 0;
			}
			else {
				x0 = d.indexOf(k0);
				if (x0 < 0) throw new IllegalArgumentException("k0 '" + k0 + "' not found in '" + d + "'.");
				l = k0.length();
			}

			if (k1 != null) {
				int x1 = d.indexOf(k1, x0 + l + 1);
				if (x1 < 0) throw new IllegalArgumentException("k1 '" + k1 + "' not found in '" + d + "'.");
				ret = d.substring(x0 + l, x1);
			}
			else {
				ret = d.substring(x0 + l);
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("data='" + d + "' k0='" + k0 + "' k1='" + k1 + "'.", e);
		}

		return ret;
	}

}
