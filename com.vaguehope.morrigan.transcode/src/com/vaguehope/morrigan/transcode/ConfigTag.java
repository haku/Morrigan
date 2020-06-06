package com.vaguehope.morrigan.transcode;

import java.util.Date;
import java.util.List;

import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.ItemTags;

public enum ConfigTag {
	/**
	 *  time duration, e.g. 1:23
	 */
	TRIM_END("trim_end="),
	/**
	 * Value for ffmpeg's `-filter:a`, e.g. `dynaudnorm=s=4`.
	 */
	AUDIO_FILTER("af="),
	;

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
	public ItemConfigTag read(final ItemTags tags) {
		final List<MediaTag> matchingTags = tags.startingWith(this.prefix);
		if (matchingTags.size() < 1) return null;

		// Look for newest undeleted tag.
		MediaTag newest = null;
		for (final MediaTag tag : matchingTags) {
			if (!tag.isDeleted() && tag.isNewerThan(newest)) newest = tag;
		}

		// If nothing undeleted, how about newest deleted?
		if (newest == null) {
			for (final MediaTag tag : matchingTags) {
				if (tag.isNewerThan(newest)) newest = tag;
			}
		}

		return new ItemConfigTag(this, newest);
	}

	public static boolean isAnyPresent(final ItemTags tags) {
		for (final ConfigTag ct : ConfigTag.values()) {
			final ItemConfigTag ict = ct.read(tags);
			if (ict != null && ict.isPresent()) return true;
		}
		return false;
	}

	public static Date newest (final ItemTags tags) {
		Date newest = null;
		for (final ConfigTag ct : ConfigTag.values()) {
			final ItemConfigTag ict = ct.read(tags);
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
