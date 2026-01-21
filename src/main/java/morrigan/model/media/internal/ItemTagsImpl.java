package morrigan.model.media.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import morrigan.model.exceptions.MorriganException;
import morrigan.model.media.ItemTags;
import morrigan.model.media.MediaItem;
import morrigan.model.media.MediaList;
import morrigan.model.media.MediaTag;
import morrigan.model.media.MediaTagType;
import morrigan.util.StringHelper;

public class ItemTagsImpl implements ItemTags {

	private final List<MediaTag> tags;

	private ItemTagsImpl (final List<MediaTag> tags) {
		this.tags = Collections.unmodifiableList(tags);
	}

	/**
	 * Will not return null.
	 */
	public static ItemTags forItem (final MediaList list, final MediaItem item) throws MorriganException {
		if (list == null || item == null) return EMPTY;
		return new ItemTagsImpl(list.getTagsIncludingDeleted(item));
	}

	@Override
	public List<MediaTag> tagsIncludingDeleted () {
		return this.tags;
	}

	/**
	 * Will return empty list, not null.
	 */
	@Override
	public List<MediaTag> startingWith (final String startsWith) {
		final List<MediaTag> ret = new ArrayList<>();
		for (final MediaTag tag : this.tags) {
			if (!tag.isDeleted()
					&& tag.getType() == MediaTagType.MANUAL
					&& StringHelper.startsWithIgnoreCase(tag.getTag(), startsWith)) {
				ret.add(tag);
			}
		}
		return ret;
	}

}
