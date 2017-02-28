package com.vaguehope.morrigan.model.media.test;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagType;

public class Tag {

	private final String tag;
	private final MediaTagType type;
	private final String cls;
	private final Long modified;
	private final boolean deleted;

	public Tag (final String tag, final MediaTagType type, final String cls, final boolean deleted) {
		this(tag, type, cls, System.currentTimeMillis(), deleted);
	}

	public Tag (final String tag, final MediaTagType type, final String cls, final Long modified, final boolean deleted) {
		this.tag = tag;
		this.type = type;
		this.cls = cls;
		this.modified = modified;
		this.deleted = deleted;
	}

	public void addTo (final IMediaItemList<?> list, final IMixedMediaItem item) throws MorriganException {
		final Date date = this.modified == null ? null : new Date(this.modified);
		list.addTag(item, this.tag, this.type, this.cls, date, this.deleted);
	}

	public Tag withModifiedOffset (final long offset) {
		if (this.modified == null) throw new IllegalStateException("Can not offset null.");
		return new Tag(this.tag, this.type, this.cls, this.modified + offset, this.deleted);
	}

	public Tag withCurrentModified () {
		return new Tag(this.tag, this.type, this.cls, System.currentTimeMillis(), this.deleted);
	}

	public Tag withNullModified () {
		return new Tag(this.tag, this.type, this.cls, null, this.deleted);
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

	public Long getModified () {
		return this.modified;
	}

	public boolean isDeleted () {
		return this.deleted;
	}

	public static void assertTags (final List<MediaTag> actual, final Tag... expected) {
		final Set<String> actualSummary = new HashSet<String>();
		for (final MediaTag a : actual) {
			actualSummary.add(summariseTag(a));
		}
		final Set<String> expectedSummary = new HashSet<String>();
		for (final Tag e : expected) {
			if (e != null) expectedSummary.add(summariseTag(e));
		}
		assertEquals(expectedSummary, actualSummary);
	}

	private static String summariseTag (final MediaTag t) {
		return summariseTag(t.getTag(), t.getType(),
				t.getClassification() != null ? t.getClassification().getClassification() : null,
				t.getModified() != null ? t.getModified().getTime() : null, t.isDeleted());
	}

	private static String summariseTag (final Tag t) {
		return summariseTag(t.getTag(), t.getType(), t.getCls(), t.getModified(), t.isDeleted());
	}

	private static String summariseTag (final String tag, final MediaTagType type, final String cls, final Long modified, final boolean deleted) {
		return String.format("{%s|%s|%s|%s|%s}", tag, type, cls, modified, deleted);
	}

}
