package com.vaguehope.morrigan.rpc.client;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;

public class RpcTag implements MediaTag {

	private final MediaToadProto.MediaTag rpcTag;

	public static List<MediaTag> convertTags(final List<MediaToadProto.MediaTag> tags) {
		return tags.stream().map(t -> new RpcTag(t)).collect(ImmutableList.toImmutableList());
	}

	public RpcTag(final MediaToadProto.MediaTag rpcTag) {
		this.rpcTag = rpcTag;
	}

	@Override
	public String getTag() {
		return this.rpcTag.getTag();
	}

	@Override
	public MediaTagType getType() {
		if (StringUtils.isNotBlank(this.rpcTag.getCls())) return MediaTagType.AUTOMATIC;
		return MediaTagType.MANUAL;
	}

	@Override
	public MediaTagClassification getClassification() {
		return new MTC(this.rpcTag.getCls());
	}

	@Override
	public Date getModified() {
		return new Date(this.rpcTag.getModifiedMillis());
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	@Override
	public boolean isNewerThan(final MediaTag b) {
		if (b == null) return true;
		if (this.rpcTag.getModifiedMillis() <= 0) return false;

		final Date bm = b.getModified();
		if (bm == null) return true;
		return this.rpcTag.getModifiedMillis() > bm.getTime();
	}

	@Override
	public String toString() {
		return String.format("RpcTag{%s}", this.rpcTag);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.rpcTag);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof RpcTag)) return false;
		final RpcTag that = (RpcTag) obj;
		return Objects.equals(this.rpcTag, that.rpcTag);
	}

	private static class MTC implements MediaTagClassification {

		private final String cls;

		public MTC(final String cls) {
			this.cls = cls;
		}

		@Override
		public String getClassification() {
			return this.cls;
		}

		@Override
		public long getDbRowId() {
			throw new UnsupportedOperationException("RpcTags do not have DB row IDs.");
		}

		@Override
		public boolean setDbRowId(final long dbRowId) {
			throw new UnsupportedOperationException("Immutable.");
		}

	}

	@Override
	public long getDbRowId() {
		throw new UnsupportedOperationException("RpcTags do not have DB row IDs.");
	}

	@Override
	public boolean setDbRowId(final long dbRowId) {
		throw new UnsupportedOperationException("Immutable.");
	}

}
