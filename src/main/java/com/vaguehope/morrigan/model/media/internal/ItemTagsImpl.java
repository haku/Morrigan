package com.vaguehope.morrigan.model.media.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.util.StringHelper;

public class ItemTagsImpl implements ItemTags {

	private final List<MediaTag> tags;

	private ItemTagsImpl (final List<MediaTag> tags) {
		this.tags = Collections.unmodifiableList(tags);
	}

	/**
	 * Will not return null.
	 */
	public static ItemTags forItem (final IMediaItemList<?> list, final IDbItem item) throws MorriganException {
		if (list == null || item == null) return EMPTY;
		return new ItemTagsImpl(list.getTagsIncludingDeleted(item));
	}

	public List<MediaTag> tagsIncludingDeleted () {
		return this.tags;
	}

	/**
	 * Will return empty list, not null.
	 */
	@Override
	public List<MediaTag> startingWith (final String startsWith) {
		final List<MediaTag> ret = new ArrayList<MediaTag>();
		for (final MediaTag tag : this.tags) {
			if (tag.getType() == MediaTagType.MANUAL && StringHelper.startsWithIgnoreCase(tag.getTag(), startsWith)) {
				ret.add(tag);
			}
		}
		return ret;
	}

}
