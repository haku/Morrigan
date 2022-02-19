package com.vaguehope.morrigan.dlna.extcd;

import java.util.Date;

import com.vaguehope.morrigan.model.media.IMixedMediaItem;

public class Metadata {

	private final Date dateLastPlayed;
	private final long startCount;
	private final long endCount;

	public static final Metadata EMPTY = new Metadata(null, 0, 0);

	private Metadata (final Date dateLastPlayed, final long startCount, final long endCount) {
		this.dateLastPlayed = dateLastPlayed;
		this.startCount = startCount;
		this.endCount = endCount;
	}

	public Metadata (final IMixedMediaItem item) {
		this.dateLastPlayed = item.getDateLastPlayed();
		this.startCount = item.getStartCount();
		this.endCount = item.getEndCount();
	}

	public Date getDateLastPlayed () {
		return this.dateLastPlayed;
	}

	public long getStartCount () {
		return this.startCount;
	}

	public long getEndCount () {
		return this.endCount;
	}

	public boolean isLessThan (final Metadata that) {
		if (that == null) return false;
		return aBeforeB(this.getDateLastPlayed(), that.getDateLastPlayed())
				|| this.endCount < that.endCount
				|| this.startCount < that.startCount;
	}

	private static boolean aBeforeB (final Date a, final Date b) {
		return (a != null ? a.getTime() : Long.MIN_VALUE) < (b != null ? b.getTime() : Long.MIN_VALUE);
	}

	@Override
	public String toString () {
		return String.format("Metadata{%s, %s, %s}", this.dateLastPlayed, this.startCount, this.endCount);
	}

}
