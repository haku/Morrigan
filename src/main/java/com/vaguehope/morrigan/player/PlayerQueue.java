package com.vaguehope.morrigan.player;

import java.util.List;

import com.vaguehope.morrigan.model.media.DurationData;

public interface PlayerQueue {

	/**
	 * Increments every time queue changes.
	 */
	long getVersion ();

	/**
	 * May return null.
	 */
	PlayItem takeFromQueue();

	PlayItem makeMetaItem(PlayItemType type);

	void addToQueue(PlayItem item);
	void addToQueue(List<PlayItem> items);
	void addToQueueTop(PlayItem item);
	void addToQueueTop(List<PlayItem> items);
	void removeFromQueue(PlayItem item);
	void clearQueue();

	void moveInQueue(List<PlayItem> items, boolean moveDown);
	void moveInQueueEnd(List<PlayItem> items, boolean toBottom);

	int size();
	List<PlayItem> getQueueList();
	void setQueueList (List<PlayItem> items);
	void shuffleQueue ();
	DurationData getQueueTotalDuration();
	/**
	 * May return null.
	 */
	PlayItem getQueueItemById (int id);

	void addQueueChangeListener(Runnable listener);
	void removeQueueChangeListener(Runnable listener);

}
