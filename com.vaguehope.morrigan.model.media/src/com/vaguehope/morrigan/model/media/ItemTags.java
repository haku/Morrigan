package com.vaguehope.morrigan.model.media;

import java.util.Collections;
import java.util.List;

public interface ItemTags {

	public List<MediaTag> tagsIncludingDeleted ();

	/**
	 * Will return empty list, not null.
	 * May include deleted tags.
	 */
	public List<MediaTag> startingWith (final String startsWith);

	public final ItemTags EMPTY = new ItemTags() {
		@Override
		public List<MediaTag> tagsIncludingDeleted () {
			return Collections.emptyList();
		}

		@Override
		public List<MediaTag> startingWith (final String startsWith) {
			return Collections.emptyList();
		}
	};

}
