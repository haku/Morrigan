package com.vaguehope.morrigan.android.helper;

public final class Objs {

	private Objs () {
		throw new AssertionError();
	}

	public static boolean equal(final Object aThis, final Object aThat){
		return aThis == null ? aThat == null : aThis.equals(aThat);
	}

}
