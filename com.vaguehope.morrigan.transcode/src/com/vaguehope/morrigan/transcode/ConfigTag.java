package com.vaguehope.morrigan.transcode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

	/**
	 * Note: Will return an object for a deleted tag.
	 */
	public ItemConfigTag read(final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) throws MorriganException {
		final List<MediaTag> tags = findTagsStartingWith(list, item, this.prefix);
		if (tags.size() < 1) return null;

		// Look for newest undeleted tag.
		MediaTag newest = null;
		for (final MediaTag tag : tags) {
			if (!tag.isDeleted() && tag.isNewerThan(newest)) newest = tag;
		}

		// If nothing undeleted, how about newest deleted?
		if (newest == null) {
			for (final MediaTag tag : tags) {
				if (tag.isNewerThan(newest)) newest = tag;
			}
		}

		return new ItemConfigTag(this, newest);
	}

	private List<MediaTag> findTagsStartingWith (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item, final String startsWith) throws MorriganException {
		if (list == null) return null;
		final List<MediaTag> ret = new ArrayList<MediaTag>();
		for (final MediaTag tag : list.getTagsIncludingDeleted(item)) {
			if (tag.getType() == MediaTagType.MANUAL && StringHelper.startsWithIgnoreCase(tag.getTag(), startsWith)) {
				ret.add(tag);
			}
		}
		return ret;
	}

	public static boolean isAnyPresent(final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) throws MorriganException {
		for (final ConfigTag ct : ConfigTag.values()) {
			// TODO optimise this to not re-fetch the tags each time?
			final ItemConfigTag ict = ct.read(list, item);
			if (ict != null && ict.isPresent()) return true;
		}
		return false;
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
