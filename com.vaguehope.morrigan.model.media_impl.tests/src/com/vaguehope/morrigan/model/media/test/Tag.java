package com.vaguehope.morrigan.model.media.test;

import java.util.Date;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.MediaTagType;

public class Tag {

	private static final long NULL_MODIFIED = -1;

	private final String tag;
	private final MediaTagType type;
	private final String cls;
	private final long modified;
	private final boolean deleted;

	public Tag (final String tag, final MediaTagType type, final String cls, final boolean deleted) {
		this(tag, type, cls, System.currentTimeMillis(), deleted);
	}

	private Tag (final String tag, final MediaTagType type, final String cls, final long modified, final boolean deleted) {
		this.tag = tag;
		this.type = type;
		this.cls = cls;
		this.modified = modified;
		this.deleted = deleted;
	}

	public void addTo (final IMediaItemList<?> list, final IMixedMediaItem item) throws MorriganException {
		final Date date = this.modified == NULL_MODIFIED ? null : new Date(this.modified);
		list.addTag(item, this.tag, this.type, this.cls, date, this.deleted);
	}

	public Tag withModifiedOffset (final long offset) {
		return new Tag(this.tag, this.type, this.cls, this.modified + offset, this.deleted);
	}

	public Tag withCurrentModified () {
		return new Tag(this.tag, this.type, this.cls, System.currentTimeMillis(), this.deleted);
	}

	public Tag withNullModified () {
		return new Tag(this.tag, this.type, this.cls, NULL_MODIFIED, this.deleted);
	}

	public Tag withDeleted (final boolean newDeleted) {
		return new Tag(this.tag, this.type, this.cls, this.modified, newDeleted);
	}

	public String getTag () {
		return this.tag;
	}

	public MediaTagType getType () {
		return this.type;
	}

	public String getCls () {
		return this.cls;
	}

	public long getModified () {
		return this.modified;
	}

	public boolean isDeleted () {
		return this.deleted;
	}

}
