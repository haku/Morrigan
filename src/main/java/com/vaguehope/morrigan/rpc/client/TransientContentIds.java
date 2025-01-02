package com.vaguehope.morrigan.rpc.client;

import java.util.UUID;

import com.google.common.base.Objects;
import com.vaguehope.morrigan.util.LruCache;

public class TransientContentIds {

	final LruCache<String, TargetAndItemIds> cache = new LruCache<>(100);

	public TransientContentIds() {
	}

	public String makeId(String targetId, String itemId) {
		final String id = UUID.randomUUID().toString();
		this.cache.put(id, new TargetAndItemIds(targetId, itemId));
		return id;
	}

	public TargetAndItemIds resolve(final String id) {
		return this.cache.get(id);
	}

	public class TargetAndItemIds {
		final String targetId;
		final String itemId;

		public TargetAndItemIds(String targetId, String itemId) {
			this.targetId = targetId;
			this.itemId = itemId;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.targetId, this.itemId);
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null) return false;
			if (this == obj) return true;
			if (!(obj instanceof TargetAndItemIds)) return false;
			final TargetAndItemIds that = (TargetAndItemIds) obj;

			return Objects.equal(this.targetId, that.targetId)
					&& Objects.equal(this.itemId, that.itemId);
		}

	}

}
