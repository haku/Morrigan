package com.vaguehope.morrigan.player;

import java.io.File;
import java.util.Objects;

import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaList;

/**
 * Will always have at least a list or an track.
 */
public class PlayItem {

	private final PlayItemType type;
	private final MediaList list;
	private final MediaItem track;
	private final File altFile;
	private int id = Integer.MIN_VALUE;

	/**
	 * Either list or track may be null.
	 */
	public PlayItem (final MediaList list, final MediaItem track) {
		if (list == null && track == null) throw new IllegalArgumentException("At least one of list and track must be specified.");
		this.type = PlayItemType.PLAYABLE;
		this.list = list;
		this.track = track;
		this.altFile = null;
	}

	protected PlayItem (final PlayItemType type) {
		if (!type.isPseudo()) throw new IllegalArgumentException("Not a meta type: " + type);
		this.type = type;
		this.list = null;
		this.track = null;
		this.altFile = null;
	}

	private PlayItem (final PlayItemType type, final MediaList list, final MediaItem track, final File altFile) {
		this.type = type;
		this.list = list;
		this.track = track;
		this.altFile = altFile;
	}

	public PlayItemType getType () {
		return this.type;
	}

	/**
	 * Has both list and tack.
	 */
	public boolean isComplete () {
		return hasList() && hasTrack();
	}

	public boolean hasList () {
		return this.list != null;
	}

	public boolean hasTrack () {
		return this.track != null;
	}

	public boolean hasAltFile () {
		return this.altFile != null;
	}

	public MediaList getList () {
		return this.list;
	}

	public MediaItem getTrack () {
		return this.track;
	}

	public File getAltFile() {
		return this.altFile;
	}

	/**
	 * Will throw if id is already set.
	 */
	public void setId (final int id) {
		if (this.id != Integer.MIN_VALUE) throw new IllegalStateException("ID is already set.");
		this.id = id;
	}

	/**
	 * Will throw if id is not already set.
	 */
	public int getId () {
		if (this.id == Integer.MIN_VALUE) throw new IllegalStateException("ID is not set.");
		return this.id;
	}

	public boolean hasId () {
		return this.id != Integer.MIN_VALUE;
	}

	public PlayItem withoutId() {
		return new PlayItem(this.type, this.list, this.track, this.altFile);
	}

	public PlayItem withTrack(final MediaItem newTrack) {
		if (this.type.isPseudo()) throw new IllegalArgumentException("Can not add track to pseudo item.");

		final PlayItem pi = new PlayItem(this.list, newTrack);
		pi.id = this.id;
		return pi;
	}

	public PlayItem withAltFile(final File newAltFile) {
		if (newAltFile == null) throw new IllegalArgumentException("Missing altFile.");
		if (this.altFile != null) throw new IllegalArgumentException("Already has altFile: " + this.altFile);
		return new PlayItem(this.type, this.list, this.track, newAltFile);
	}

	public String resolveTitle(MediaList relativeTo) {
		if (this.type.isPseudo()) return this.type.toString();
		if (this.track == null) return this.list.getListName();
		if (this.list.equals(relativeTo)) return this.track.getTitle();
		return this.list.getListName() + "/" + this.track.getTitle();
	}

	@Override
	public String toString () {
		return resolveTitle(null);
	}

	@Override
	public boolean equals (final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof PlayItem)) return false;
		PlayItem that = (PlayItem) obj;

		return Objects.equals(this.type, that.type)
				&& this.id == that.id
				&& Objects.equals(this.list, that.list)
				&& Objects.equals(this.track, that.track);
	}

	@Override
	public int hashCode () {
		return Objects.hash(this.type, this.list, this.track, this.id);
	}

}
