package com.vaguehope.morrigan.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaguehope.morrigan.model.media.DurationData;

public class DefaultPlayerQueue implements PlayerQueue {

	private final AtomicInteger queueId = new AtomicInteger(0);
	private final List<PlayItem> queue = new CopyOnWriteArrayList<PlayItem>();
	private final List<Runnable> queueChangeListeners = Collections.synchronizedList(new ArrayList<Runnable>());

	public DefaultPlayerQueue () {}

	private void validateQueueItemBeforeAdd (final PlayItem item) {
		if (item.item != null && !item.item.isPlayable()) throw new IllegalArgumentException("item is not playable.");
		if (item.id >= 0) throw new IllegalArgumentException("item can not already have id.");
		item.id = this.queueId.getAndIncrement();
	}

	@Override
	public void addToQueue (final PlayItem item) {
		validateQueueItemBeforeAdd(item);
		this.queue.add(item);
		callQueueChangedListeners();
	}

	@Override
	public void addToQueue (final List<PlayItem> items) {
		for (final PlayItem item : items) {
			validateQueueItemBeforeAdd(item);
		}
		this.queue.addAll(items);
		callQueueChangedListeners();
	}

	@Override
	public void removeFromQueue (final PlayItem item) {
		this.queue.remove(item);
		callQueueChangedListeners();
	}

	@Override
	public void clearQueue () {
		this.queue.clear();
		callQueueChangedListeners();
	}

	@Override
	public void moveInQueue (final List<PlayItem> items, final boolean moveDown) {
		synchronized (this.queue) {
			if (items == null || items.isEmpty()) return;

			for (int i = (moveDown ? this.queue.size() - 1 : 0); (moveDown ? i >= 0 : i < this.queue.size()); i = i + (moveDown ? -1 : 1)) {
				if (items.contains(this.queue.get(i))) {
					int j;
					if (moveDown) {
						if (i == this.queue.size() - 1) {
							j = -1;
						}
						else {
							j = i + 1;
						}
					}
					else {
						if (i == 0) {
							j = -1;
						}
						else {
							j = i - 1;
						}
					}
					if (j != -1 && !items.contains(this.queue.get(j))) {
						final PlayItem a = this.queue.get(i);
						final PlayItem b = this.queue.get(j);
						this.queue.set(i, b);
						this.queue.set(j, a);
					}
				}
			}
		}
		callQueueChangedListeners();
	}

	@Override
	public void moveInQueueEnd (final List<PlayItem> items, final boolean toBottom) {
		// TODO This could probably be done better.
		synchronized (this.queue) {
			final List<PlayItem> ret = new ArrayList<PlayItem>(this.queue.size());
			if (!toBottom) ret.addAll(items);
			for (final PlayItem item : this.queue) {
				if (!items.contains(item)) ret.add(item);
			}
			if (toBottom) ret.addAll(items);
			this.setQueueList(ret);
		}
	}

	@Override
	public PlayItem takeFromQueue () {
		synchronized (this.queue) {
			if (!this.queue.isEmpty()) {
				final PlayItem item = this.queue.remove(0);
				callQueueChangedListeners();
				return item;
			}
			return null;
		}
	}

	private void callQueueChangedListeners () {
		// TODO upgrade to use RunHelper.
		synchronized (this.queueChangeListeners) {
			for (final Runnable r : this.queueChangeListeners) {
				r.run();
			}
		}
	}

	@Override
	public List<PlayItem> getQueueList () {
		synchronized (this.queue) {
			return Collections.unmodifiableList(this.queue);
		}
	}

	@Override
	public void setQueueList (final List<PlayItem> items) {
		synchronized (this.queue) {
			this.queue.clear();
			this.queue.addAll(items);
		}
		callQueueChangedListeners();
	}

	@Override
	public void shuffleQueue () {
		synchronized (this.queue) {
			Collections.shuffle(this.queue);
		}
		callQueueChangedListeners();
	}

	@Override
	public DurationData getQueueTotalDuration () {
		boolean complete = true;
		long duration = 0;
		synchronized (this.queue) {
			for (final PlayItem pi : this.queue) {
				if (pi.item != null && pi.item.getDuration() > 0) {
					duration = duration + pi.item.getDuration();
				}
				else {
					complete = false;
				}
			}
		}
		return new DurationDataImpl(duration, complete);
	}

	@Override
	public PlayItem getQueueItemById (final int itemId) {
		// TODO Is there a better way to do this?
		final Map<Integer, PlayItem> q = new HashMap<Integer, PlayItem>();
		synchronized (this.queue) {
			for (final PlayItem item : this.queue) {
				q.put(Integer.valueOf(item.id), item);
			}
			return q.get(Integer.valueOf(itemId));
		}
	}

	@Override
	public void addQueueChangeListener (final Runnable listener) {
		this.queueChangeListeners.add(listener);
	}

	@Override
	public void removeQueueChangeListener (final Runnable listener) {
		this.queueChangeListeners.remove(listener);
	}

}
