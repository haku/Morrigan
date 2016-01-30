package com.vaguehope.morrigan.player.internal;

import java.util.Collections;
import java.util.List;

import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.player.DurationDataImpl;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerQueue;

public enum ImmutableEmptyPlayerQueue implements PlayerQueue {
	INSTANCE;

	@Override
	public long getVersion () {
		return 1L;
	}

	@Override
	public PlayItem takeFromQueue () {
		return null;
	}

	@Override
	public void addToQueue (final PlayItem item) {
		// NOOP.
	}

	@Override
	public void addToQueue (final List<PlayItem> item) {
		// NOOP.
	}

	@Override
	public void removeFromQueue (final PlayItem item) {
		// NOOP.
	}

	@Override
	public void clearQueue () {
		// NOOP.
	}

	@Override
	public void moveInQueue (final List<PlayItem> items, final boolean moveDown) {
		// NOOP.
	}

	@Override
	public void moveInQueueEnd (final List<PlayItem> items, final boolean toBottom) {
		// NOOP.
	}

	@Override
	public int size () {
		return 0;
	}

	@Override
	public List<PlayItem> getQueueList () {
		return Collections.emptyList();
	}

	@Override
	public void setQueueList (final List<PlayItem> items) {
		// NOOP.
	}

	@Override
	public void shuffleQueue () {
		// NOOP.
	}

	@Override
	public DurationData getQueueTotalDuration () {
		return new DurationDataImpl(0, true);
	}

	@Override
	public PlayItem getQueueItemById (final int id) {
		return null;
	}

	@Override
	public void addQueueChangeListener (final Runnable listener) {
		// NOOP.
	}

	@Override
	public void removeQueueChangeListener (final Runnable listener) {
		// NOOP.
	}

}
