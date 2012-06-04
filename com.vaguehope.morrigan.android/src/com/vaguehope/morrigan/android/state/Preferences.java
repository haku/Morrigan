package com.vaguehope.morrigan.android.state;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public final class Preferences {

	private static final String CURRENT_SERVER = "current_server";

	private Preferences () {}

	public static void putCurrentServer (Context context, int serverId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = prefs.edit();
		edit.putInt(CURRENT_SERVER, serverId);
		edit.commit();
	}

	public static int getCurrentServer (Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getInt(CURRENT_SERVER, -1);
	}

}
