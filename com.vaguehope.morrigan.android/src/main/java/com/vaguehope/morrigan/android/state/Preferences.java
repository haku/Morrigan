package com.vaguehope.morrigan.android.state;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public final class Preferences {

	private static final String CURRENT_SERVER = "current_server";

	private Preferences () {}

	public static void putCurrentServer (final Context context, final String serverId) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final Editor edit = prefs.edit();
		edit.putString(CURRENT_SERVER, serverId);
		edit.commit();
	}

	public static String getCurrentServer (final Context context) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		try {
			return prefs.getString(CURRENT_SERVER, null);
		}
		catch (final ClassCastException e) {
			return "" + prefs.getInt(CURRENT_SERVER, -1);
		}
	}

}
