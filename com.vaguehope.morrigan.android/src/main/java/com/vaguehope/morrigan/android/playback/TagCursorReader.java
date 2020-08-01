package com.vaguehope.morrigan.android.playback;

import android.database.Cursor;

public class TagCursorReader {

	private int colId = -1;
	private int colMfRowId = -1;
	private int colTag = -1;
	private int colCls = -1;
	private int colType = -1;
	private int colModified = -1;
	private int colDeleted = -1;

	public long readId (final Cursor c) {
		if (c == null) return -1;
		if (this.colId < 0) this.colId = c.getColumnIndexOrThrow(MediaDbImpl.TBL_TG_ID);
		return c.getLong(this.colId);
	}

	public long readMfRowId (final Cursor c) {
		if (c == null) return -1;
		if (this.colMfRowId < 0) this.colMfRowId = c.getColumnIndexOrThrow(MediaDbImpl.TBL_TG_MFID);
		return c.getLong(this.colMfRowId);
	}

	public String readTag (final Cursor c) {
		if (c == null) return null;
		if (this.colTag < 0) this.colTag = c.getColumnIndexOrThrow(MediaDbImpl.TBL_TG_TAG);
		return c.getString(this.colTag);
	}

	public String readCls (final Cursor c) {
		if (c == null) return null;
		if (this.colCls < 0) this.colCls = c.getColumnIndexOrThrow(MediaDbImpl.TBL_TG_CLS);
		return c.getString(this.colCls);
	}

	public MediaTagType readType (final Cursor c) {
		if (c == null) return null;
		if (this.colType < 0) this.colType = c.getColumnIndexOrThrow(MediaDbImpl.TBL_TG_TYPE);
		return MediaTagType.getFromNumber(c.getInt(this.colType));
	}

	public long readModified (final Cursor c) {
		if (c == null) return -1;
		if (this.colModified < 0) this.colModified = c.getColumnIndexOrThrow(MediaDbImpl.TBL_TG_MODIFIED);
		return c.getLong(this.colModified);
	}

	public boolean readDeleted (final Cursor c) {
		if (c == null) return false;
		if (this.colDeleted < 0) this.colDeleted = c.getColumnIndexOrThrow(MediaDbImpl.TBL_TG_DELETED);
		return !c.isNull(this.colDeleted);
	}

	public MediaTag readItem (final Cursor c) {
		if (c == null) return null;
		return new MediaTag(readId(c), readTag(c), readCls(c), readType(c), readModified(c), readDeleted(c));
	}

}
