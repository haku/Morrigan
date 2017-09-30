package com.vaguehope.morrigan.android.playback;

import android.database.Cursor;

public class MediaCursorReader {

	private int colTitle = -1;

	public String readTitle (final Cursor c) {
		if (c == null) return null;
		if (this.colTitle < 0) this.colTitle = c.getColumnIndexOrThrow(MediaDbImpl.TBL_MF_TITLE);
		return c.getString(this.colTitle);
	}

}
