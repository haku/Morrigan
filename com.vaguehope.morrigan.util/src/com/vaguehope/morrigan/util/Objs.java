package com.vaguehope.morrigan.util;

import java.util.Arrays;

public final class Objs {

	private Objs () {}

	public static boolean equals (final Object a, final Object b) {
		return (a == b) || (a != null && a.equals(b));
	}

	public static int hash (final Object obj) {
		return obj != null ? obj.hashCode() : 0;
	}

	public static int hash (final Object... values) {
		return Arrays.hashCode(values);
	}

}
