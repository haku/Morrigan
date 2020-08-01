package com.vaguehope.morrigan.android.playback;

import com.vaguehope.morrigan.android.helper.Objs;
import com.vaguehope.morrigan.android.helper.StringHelper;

public class MediaTag {

	private final long rowId;
	private final String tag;
	private final String cls;
	private final MediaTagType type;
	private final long modified;
	private final boolean deleted;

	public MediaTag (final String tag, final String cls, final MediaTagType type, final long modified, final boolean deleted) {
		this(-1, tag, cls, type, modified, deleted);
	}

	public MediaTag (final long rowId, final String tag, final String cls, final MediaTagType type, final long modified, final boolean deleted) {
		if (StringHelper.isEmpty(tag)) throw new IllegalArgumentException("type is required.");
		if (type == null) throw new IllegalArgumentException("type is required.");
		this.rowId = rowId;
		this.tag = tag;
		this.cls = cls != null ? cls : "";
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

	public boolean hasModified () {
		return this.modified > 0;
	}

	public long getModified () {
		return this.modified;
	}

	public boolean isDeleted () {
		return this.deleted;
	}

	public boolean equalValue (final MediaTag that) {
		return Objs.equal(this.tag, that.tag)
				&& Objs.equal(this.cls, that.cls)
				&& this.type == that.type
				&& this.deleted == that.deleted;
	}

	@Override
	public String toString () {
		return String.format("MediaTag{%s,%s,%s,%s,%s,%s}", this.rowId, this.tag, this.cls, this.type, this.modified, this.deleted);
	}

}
