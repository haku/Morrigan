package morrigan.player.contentproxy;

import java.util.UUID;

import com.google.common.base.Objects;

import morrigan.util.LruCache;

public class TransientContentIds {

	final LruCache<String, TransientContentItem> cache = new LruCache<>(100);

	public TransientContentIds() {
	}

	public String makeId(final String listId, final String itemId, final ContentServer contentServer) {
		final String id = UUID.randomUUID().toString();
		this.cache.put(id, new TransientContentItem(listId, itemId, contentServer));
		return id;
	}

	public TransientContentItem resolve(final String id) {
		return this.cache.get(id);
	}

	public class TransientContentItem {
		final String listId;
		final String itemId;
		final ContentServer contentServer;

		public TransientContentItem(final String listId, final String itemId, final ContentServer contentServer) {
			this.listId = listId;
			this.itemId = itemId;
			this.contentServer = contentServer;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.listId, this.itemId, this.contentServer);
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null) return false;
			if (this == obj) return true;
			if (!(obj instanceof TransientContentItem)) return false;
			final TransientContentItem that = (TransientContentItem) obj;

			return Objects.equal(this.listId, that.listId)
					&& Objects.equal(this.itemId, that.itemId)
					&& Objects.equal(this.contentServer, that.contentServer);
		}

	}

}
