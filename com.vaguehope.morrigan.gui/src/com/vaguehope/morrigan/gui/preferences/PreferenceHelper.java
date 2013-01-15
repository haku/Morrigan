package com.vaguehope.morrigan.gui.preferences;

import com.vaguehope.morrigan.gui.Activator;

public final class PreferenceHelper {

	private PreferenceHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String PREF_LASTJUMPTODLGQUERY = "PREF_LASTJUMPTODLGQUERY";

	public static String getLastJumpToDlgQuery () {
		String s = Activator.getDefault().getPreferenceStore().getString(PREF_LASTJUMPTODLGQUERY);
		return s;
	}

	public static void setLastJumpToDlgQuery (String q) {
		Activator.getDefault().getPreferenceStore().setValue(PREF_LASTJUMPTODLGQUERY, q);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
