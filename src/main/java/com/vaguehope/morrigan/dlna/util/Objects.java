package com.vaguehope.morrigan.dlna.util;

public class Objects {

	private Objects () {
		throw new AssertionError();
	}

	public static boolean equals (final Object a, final Object b) {
		return (a == b) || (a != null && a.equals(b));
	}

}
