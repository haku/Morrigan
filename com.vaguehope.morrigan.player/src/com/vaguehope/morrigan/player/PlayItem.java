package com.vaguehope.morrigan.player;

import com.vaguehope.morrigan.model.helper.EqualHelper;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;

/**
 * Will always have at least a list or an track.
 */
public class PlayItem {

	private final IMediaTrackList<? extends IMediaTrack> list;
	private final IMediaTrack track;
	private int id = Integer.MIN_VALUE;

	/**
	 * Either list or track may be null.
	 */
	public PlayItem (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack track) {
		if (list == null && track == null) throw new IllegalArgumentException("At least one of list and track must be specified.");
		this.list = list;
		this.track = track;
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

	public PlayItem withTrack(final IMediaTrack newTrack) {
		final PlayItem pi = new PlayItem(this.list, newTrack);
		pi.id = this.id;
		return pi;
	}

	@Override
	public String toString () {
		if (this.track == null) return this.list.getListName();
		return this.list.getListName() + "/" + this.track.getTitle();
	}

	@Override
	public boolean equals (final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof PlayItem)) return false;
		PlayItem that = (PlayItem) obj;

		return EqualHelper.areEqual(this.list, that.list)
				&& EqualHelper.areEqual(this.track, that.track)
				&& this.id == that.id;
	}

	@Override
	public int hashCode () {
		int hash = 1;
		hash = hash * 31 + (this.list == null ? 0 : this.list.hashCode());
		hash = hash * 31 + (this.track == null ? 0 : this.track.hashCode());
		hash = hash * 31 + this.id;
		return hash;
	}

}
