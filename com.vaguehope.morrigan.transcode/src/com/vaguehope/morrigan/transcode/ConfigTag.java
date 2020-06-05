package com.vaguehope.morrigan.transcode;

import java.util.Date;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.util.StringHelper;

public enum ConfigTag {
	TRIM_END("trim_end=");

	private final String prefix;

	private ConfigTag (final String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix () {
		return this.prefix;
	}

	public ItemConfigTag read(final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) throws MorriganException {
		final MediaTag tag = findTagStartingWith(list, item, this.prefix);
		if (tag == null) return null;
		return new ItemConfigTag(this, tag);
	}

	private MediaTag findTagStartingWith (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item, final String startsWith) throws MorriganException {
		if (list == null) return null;
		for (final MediaTag tag : list.getTags(item)) {
			if (tag.getType() == MediaTagType.MANUAL && StringHelper.startsWithIgnoreCase(tag.getTag(), startsWith)) {
				return tag;
			}
		}
		return null;
	}

	public static Date newest (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) throws MorriganException {
		Date newest = null;
		for (final ConfigTag ct : ConfigTag.values()) {
			// TODO optimise this to not re-fetch the tags each time?
			final ItemConfigTag ict = ct.read(list, item);
			final Date ictModified = ict != null ? ict.getLastModified() : null;
			if (ictModified != null) {
				if (newest == null || ictModified.getTime() > newest.getTime()) {
					newest = ictModified;
				}
			}
		}
		return newest;
	}
}
