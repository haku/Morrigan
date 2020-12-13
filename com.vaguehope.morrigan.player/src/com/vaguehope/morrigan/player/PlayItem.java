package com.vaguehope.morrigan.player;

import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.util.Objs;

/**
 * Will always have at least a list or an track.
 */
public class PlayItem {

	private final PlayItemType type;
	private final IMediaTrackList<? extends IMediaTrack> list;
	private final IMediaTrack track;
	private int id = Integer.MIN_VALUE;

	/**
	 * Either list or track may be null.
	 */
	public PlayItem (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack track) {
		if (list == null && track == null) throw new IllegalArgumentException("At least one of list and track must be specified.");
		this.type = PlayItemType.PLAYABLE;
		this.list = list;
		this.track = track;
	}

	protected PlayItem (final PlayItemType type) {
		if (!type.isPseudo()) throw new IllegalArgumentException("Not a meta type: " + type);
		this.type = type;
		this.list = null;
		this.track = null;
	}

	private PlayItem (final PlayItemType type, final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack track) {
		this.type = type;
		this.list = list;
		this.track = track;
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

	public IMediaTrackList<? extends IMediaTrack> getList () {
		return this.list;
	}

	public IMediaTrack getTrack () {
		return this.track;
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
		return new PlayItem(this.type, this.list, this.track);
	}

	public PlayItem withTrack(final IMediaTrack newTrack) {
		if (this.type.isPseudo()) throw new IllegalArgumentException("Can not add track to pseudo item.");

		final PlayItem pi = new PlayItem(this.list, newTrack);
		pi.id = this.id;
		return pi;
	}

	@Override
	public String toString () {
		if (this.type.isPseudo()) return this.type.toString();
		if (this.track == null) return this.list.getListName();
		return this.list.getListName() + "/" + this.track.getTitle();
	}

	@Override
	public boolean equals (final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof PlayItem)) return false;
		PlayItem that = (PlayItem) obj;

		return Objs.equals(this.type, that.type)
				&& this.id == that.id
				&& Objs.equals(this.list, that.list)
				&& Objs.equals(this.track, that.track);
	}

	@Override
	public int hashCode () {
		return Objs.hash(this.type, this.list, this.track, this.id);
	}

}
