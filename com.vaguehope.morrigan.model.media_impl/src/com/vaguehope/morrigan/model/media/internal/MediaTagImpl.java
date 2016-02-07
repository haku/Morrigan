package com.vaguehope.morrigan.model.media.internal;

import java.util.Date;

import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;

public class MediaTagImpl implements MediaTag {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final long dbRowId;
	private final String tag;
	private final MediaTagType type;
	private final MediaTagClassification classification;
	private final Date modified;
	private final boolean deleted;

	public MediaTagImpl (final long dbRowId, final String tag, final MediaTagType type, final MediaTagClassification classification, final Date modified, final boolean deleted) {
		this.dbRowId = dbRowId;
		this.tag = tag;
		this.type = type;
		this.classification = classification;
		this.modified = modified;
		this.deleted = deleted;
	}

	@Override
	public long getDbRowId() {
		return this.dbRowId;
	}

	@Override
	public boolean setDbRowId(final long dbRowId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTag() {
		return this.tag;
	}

	@Override
	public MediaTagType getType() {
		return this.type;
	}

	@Override
	public MediaTagClassification getClassification() {
		return this.classification;
	}

	@Override
	public Date getModified () {
		return this.modified;
	}

	@Override
	public boolean isDeleted () {
		return this.deleted;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(getTag());

		if (getClassification() != null) { // Empty string != null.
			sb.append(" <");
			sb.append(getClassification());
			sb.append(">");
		}

		sb.append(" [");
		sb.append(getType().getShortName());
		sb.append("]");

		return sb.toString();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
