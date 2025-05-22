package com.vaguehope.morrigan.player;

import java.io.File;
import java.math.BigInteger;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.util.ExFunction;

public abstract class PlayItem {

	/**
	 * either list xor item may be null.
	 */
	public static PlayItem makeReady(final MediaList list, final MediaItem item) {
		if (list == null && item == null) throw new IllegalArgumentException("At least one of list and item must be specified.");
		return new ReadyPlayItem(PlayItemType.PLAYABLE, list, item, null);
	}

	public static PlayItem makeAction(final PlayItemType type) {
		if (!type.isPseudo()) throw new IllegalArgumentException("Not a meta type: " + type);
		return new ReadyPlayItem(type, null, null, null);
	}

	public static PlayItem makeUnresolved(final ListRef listRef, final String filepath, final String remoteId, final BigInteger md5, final String title) {
		return new UnresolvedPlayItem(listRef, filepath, remoteId, md5, title);
	}

	public abstract boolean isReady();
	public abstract PlayItemType getType();
	public abstract boolean hasList();
	public abstract boolean hasItem();
	public abstract boolean hasAltFile();
	public abstract ListRef getListRef();
	public abstract MediaList getList();
	public abstract MediaItem getItem();
	public abstract File getAltFile();

	public abstract String getFilepath();
	public abstract String getRemoteId();
	public abstract BigInteger getMd5();
	public abstract String getTitle();
	public abstract String getListTitle();

	public abstract PlayItem withoutId();
	public abstract PlayItem withItem(final MediaItem newItem);
	public abstract PlayItem withAltFile(final File newAltFile);
	public abstract PlayItem makeReady(final ExFunction<ListRef, MediaList, MorriganException> listSupplier) throws MorriganException;

	private static class ReadyPlayItem extends PlayItem {

		private final PlayItemType type;
		private final MediaList list;
		private final MediaItem item;
		private final File altFile;

		ReadyPlayItem(final PlayItemType type, final MediaList list, final MediaItem item, final File altFile) {
			this.type = type;
			this.list = list;
			this.item = item;
			this.altFile = altFile;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public PlayItemType getType() {
			return this.type;
		}

		@Override
		public boolean hasList() {
			return this.list != null;
		}

		@Override
		public boolean hasItem() {
			return this.item != null;
		}

		@Override
		public boolean hasAltFile() {
			return this.altFile != null;
		}

		@Override
		public ListRef getListRef() {
			if (this.list != null) return this.list.getListRef();
			return null;
		}

		@Override
		public MediaList getList() {
			return this.list;
		}

		@Override
		public MediaItem getItem() {
			return this.item;
		}

		@Override
		public File getAltFile() {
			return this.altFile;
		}

		@Override
		public String getFilepath() {
			if (this.item == null) return null;
			return this.item.getFilepath();
		}

		@Override
		public String getRemoteId() {
			if (this.item == null) return null;
			return this.item.getRemoteId();
		}

		@Override
		public BigInteger getMd5() {
			if (this.item == null) return null;
			return this.item.getMd5();
		}

		@Override
		public String getTitle() {
			if (this.type.isPseudo()) return this.type.toString();
			if (this.item == null) return this.list.getListName();
			return this.item.getTitle();
		}

		@Override
		public String getListTitle() {
			if (this.list == null) return "";
			return this.list.getListName();
		}

		@Override
		public PlayItem withoutId() {
			return new ReadyPlayItem(this.type, this.list, this.item, this.altFile);
		}

		@Override
		public PlayItem withItem(final MediaItem newItem) {
			if (this.type.isPseudo()) throw new IllegalArgumentException("Can not add item to pseudo item.");

			final PlayItem pi = new ReadyPlayItem(this.type, this.list, newItem, null);
			pi.id = this.id;
			return pi;
		}

		@Override
		public PlayItem withAltFile(final File newAltFile) {
			if (newAltFile == null) throw new IllegalArgumentException("Missing altFile.");
			return new ReadyPlayItem(this.type, this.list, this.item, newAltFile);
		}

		@Override
		public PlayItem makeReady(final ExFunction<ListRef, MediaList, MorriganException> listSupplier) throws MorriganException {
			return this;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null) return false;
			if (this == obj) return true;
			if (!(obj instanceof ReadyPlayItem)) return false;
			final ReadyPlayItem that = (ReadyPlayItem) obj;

			return this.id == that.id
					&& Objects.equals(this.type, that.type)
					&& Objects.equals(this.list, that.list)
					&& Objects.equals(this.item, that.item);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.type, this.list, this.item, this.id);
		}

		@Override
		public String toString() {
			return String.format("ReadyPlayItem{%s, %s, %s, %s}", this.id, this.type, this.list, this.item);
		}
	}

	private static class UnresolvedPlayItem extends PlayItem {

		private static final String TITLE_PREFIX = "Unresolved: ";

		private final ListRef listRef;
		private final String filepath;
		private final String remoteId;
		private final BigInteger md5;
		private final String title;

		/**
		 * either list or item may be null.
		 * @param title
		 */
		public UnresolvedPlayItem(final ListRef listRef, final String filepath, final String remoteId, final BigInteger md5, String title) {
			this.listRef = listRef;
			this.filepath = filepath;
			this.remoteId = remoteId;
			this.md5 = md5;
			this.title = title;
		}

		@Override
		public PlayItem makeReady(final ExFunction<ListRef, MediaList, MorriganException> listSupplier) throws MorriganException {
			final MediaList list = listSupplier.apply(this.listRef);
			if (list == null) return null;

			MediaItem item = null;
			if (StringUtils.isNotBlank(this.filepath)) item = list.getByFile(this.filepath);
			if (item == null && StringUtils.isNotBlank(this.remoteId)) item = list.getByFile(this.remoteId);
			if (item == null && list.canGetByMd5()) item = list.getByMd5(this.md5);
			if (item != null) return new ReadyPlayItem(PlayItemType.PLAYABLE, list, item, null);
			return null;
		}

		@Override
		public boolean isReady() {
			return false;
		}

		@Override
		public PlayItemType getType() {
			return PlayItemType.PLAYABLE;
		}

		@Override
		public boolean hasList() {
			return this.listRef != null;
		}

		@Override
		public boolean hasItem() {
			return this.filepath != null || this.remoteId != null;
		}

		@Override
		public boolean hasAltFile() {
			throw unsupported();
		}

		@Override
		public ListRef getListRef() {
			return this.listRef;
		}

		@Override
		public MediaList getList() {
			throw unsupported();
		}

		@Override
		public MediaItem getItem() {
			throw unsupported();
		}

		@Override
		public File getAltFile() {
			throw unsupported();
		}

		@Override
		public String getFilepath() {
			return this.filepath;
		}

		@Override
		public String getRemoteId() {
			return this.remoteId;
		}

		@Override
		public BigInteger getMd5() {
			return this.md5;
		}

		@Override
		public String getTitle() {
			if (StringUtils.isNotBlank(this.title)) {
				if (this.title.startsWith(TITLE_PREFIX)) return this.title;
				return TITLE_PREFIX + this.title;
			}
			return (String.format(TITLE_PREFIX + "{%s, %s}",
					this.listRef.toUrlForm(),
					this.filepath != null ? this.filepath : this.remoteId));
		}

		@Override
		public String getListTitle() {
			return String.format("(%s)", this.listRef.toUrlForm());
		}

		@Override
		public PlayItem withoutId() {
			return new UnresolvedPlayItem(this.listRef, this.filepath, this.remoteId, this.md5, this.title);
		}

		@Override
		public PlayItem withItem(final MediaItem newItem) {
			throw unsupported();
		}

		@Override
		public PlayItem withAltFile(final File newAltFile) {
			throw unsupported();
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null) return false;
			if (this == obj) return true;
			if (!(obj instanceof UnresolvedPlayItem)) return false;
			final UnresolvedPlayItem that = (UnresolvedPlayItem) obj;

			return this.id == that.id
					&& Objects.equals(this.listRef, that.listRef)
					&& Objects.equals(this.filepath, that.filepath)
					&& Objects.equals(this.remoteId, that.remoteId)
					&& Objects.equals(this.md5, that.md5)
					&& Objects.equals(this.title, that.title);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.listRef, this.filepath, this.remoteId, this.md5, this.title);
		}

		@Override
		public String toString() {
			return String.format("UnresolvedPlayItem{%s, %s, %s, %s, %s, %s}", this.id, this.listRef, this.filepath, this.remoteId, this.md5, this.title);
		}

		private static UnsupportedOperationException unsupported() {
			return new UnsupportedOperationException("Not supported on UnresolvedPlayItem.");
		}
	}

	protected int id = Integer.MIN_VALUE;

	/**
	 * Will throw if id is already set.
	 */
	public void setId(final int id) {
		if (this.id != Integer.MIN_VALUE) throw new IllegalStateException("ID is already set.");
		this.id = id;
	}

	/**
	 * Will throw if id is not already set.
	 */
	public int getId() {
		if (this.id == Integer.MIN_VALUE) throw new IllegalStateException("ID is not set.");
		return this.id;
	}

	public boolean hasId() {
		return this.id != Integer.MIN_VALUE;
	}

	public boolean hasListAndItem() {
		return hasList() && hasItem();
	}

	public PlayItem makeReady(final MediaFactory mf) throws MorriganException {
		return makeReady((listRef) -> mf.getList(listRef));
	}

}
