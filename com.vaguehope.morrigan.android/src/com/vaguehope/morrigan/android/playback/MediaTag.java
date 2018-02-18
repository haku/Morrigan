package com.vaguehope.morrigan.android.playback;

import com.vaguehope.morrigan.android.helper.StringHelper;

public class MediaTag {

	private final long rowId;
	private final String tag;
	private final String cls;
	private final MediaTagType type;
	private final long modified;
	private final boolean deleted;

	public MediaTag (final long rowId, final String tag, final String cls, final MediaTagType type, final long modified, final boolean deleted) {
		if (StringHelper.isEmpty(tag)) throw new IllegalArgumentException("type is required.");
		if (type == null) throw new IllegalArgumentException("type is required.");
		if (modified <= 0) throw new IllegalArgumentException("modified is required.");
		this.rowId = rowId;
		this.tag = tag;
		this.cls = cls;
		this.type = type;
		this.modified = modified;
		this.deleted = deleted;
	}

	public long getRowId () {
		return this.rowId;
	}

	public String getTag () {
		return this.tag;
	}

	public String getCls () {
		return this.cls;
	}

	public MediaTagType getType () {
		return this.type;
	}

	public long getModified () {
		return this.modified;
	}

	public boolean isDeleted () {
		return this.deleted;
	}

}
