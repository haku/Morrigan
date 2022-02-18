package com.vaguehope.morrigan.model.helper;

public final class EqualHelper {

	private EqualHelper () {}

	public static boolean areEqual(Object aThis, Object aThat){
		return aThis == null ? aThat == null : aThis.equals(aThat);
	}

}
