package com.vaguehope.morrigan.android.helper;

import java.util.List;

import android.content.Context;
import android.content.UriPermission;

public class ContentHelper {

	public static void logPermissions (final Context context, final LogWrapper log) {
		final List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
		log.i("Content permissions: %s", permissions.size());
		for (final UriPermission p : permissions) {
			log.i("  r=%s w=%s t=%s %s", p.isReadPermission(), p.isWritePermission(), p.getPersistedTime(), p.getUri());
		}
	}

}
