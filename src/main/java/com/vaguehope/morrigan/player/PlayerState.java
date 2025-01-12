package com.vaguehope.morrigan.player;

import java.util.List;
import java.util.Objects;

import com.vaguehope.morrigan.transcode.Transcode;

public class PlayerState {

	final PlaybackOrder playbackOrder;
	final Transcode transcode;
	final long position;
	final String listRef;
	final QueueItem item;
	final List<QueueItem> queue;

	public PlayerState(
			final PlaybackOrder playbackOrder,
			final Transcode transcode,
			final long position,
			final String listRef,
			final QueueItem item,
			final List<QueueItem> queue) {
		this.playbackOrder = playbackOrder;
		this.transcode = transcode;
		this.position = position;
		this.listRef = listRef;
		this.item = item;
		this.queue = queue;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.playbackOrder, this.transcode, this.position);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof PlayerState)) return false;
		final PlayerState that = (PlayerState) obj;

		return Objects.equals(this.playbackOrder, that.playbackOrder)
				&& Objects.equals(this.transcode, that.transcode)
				&& Objects.equals(this.position, that.position);
	}

	public static class QueueItem {
		final String listRef;
		final String filepath;
		final String remoteId;
		final String md5;
		final String title;

		public QueueItem(final String listRef, final String filepath, final String remoteId, final String md5, final String title) {
			this.listRef = listRef;
			this.filepath = filepath;
			this.remoteId = remoteId;
			this.md5 = md5;
			this.title = title;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.listRef, this.filepath, this.remoteId, this.md5, this.title);
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null) return false;
			if (this == obj) return true;
			if (!(obj instanceof QueueItem)) return false;
			final QueueItem that = (QueueItem) obj;

			return Objects.equals(this.listRef, that.listRef)
					&& Objects.equals(this.filepath, this.filepath)
					&& Objects.equals(this.remoteId, this.remoteId)
					&& Objects.equals(this.md5, that.md5)
					&& Objects.equals(this.title, that.title);
		}
	}

}
