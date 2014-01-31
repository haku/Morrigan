package com.vaguehope.morrigan.player;

import java.util.List;

import com.vaguehope.morrigan.model.media.DurationData;

public interface PlayerQueue {

	/**
	 * May return null.
	 */
	PlayItem takeFromQueue();

	void addToQueue(PlayItem item);
	void addToQueue(List<PlayItem> item);
	void removeFromQueue(PlayItem item);
	void clearQueue();

	void moveInQueue(List<PlayItem> items, boolean moveDown);
	void moveInQueueEnd(List<PlayItem> items, boolean toBottom);

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
