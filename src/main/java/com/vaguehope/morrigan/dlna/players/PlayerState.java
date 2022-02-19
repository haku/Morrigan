package com.vaguehope.morrigan.dlna.players;

import java.util.ArrayList;
import java.util.List;

import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayerQueue;
import com.vaguehope.morrigan.transcode.Transcode;

public class PlayerState {

	private final PlaybackOrder playbackOrder;
	private final Transcode transcode;
	private final PlayItem currentItem;
	private final long position;
	private final boolean isPlaying;
	private final List<PlayItem> queueItems;

	public PlayerState (
			final PlaybackOrder playbackOrder,
			final Transcode transcode,
			final PlayItem currentItem,
			final long position,
			final boolean isPlaying,
			final PlayerQueue queue) {
		this.playbackOrder = playbackOrder;
		this.transcode = transcode;
		this.currentItem = currentItem;
		this.position = position;
		this.isPlaying = isPlaying;
		this.queueItems = new ArrayList<PlayItem>(queue.getQueueList());
	}

	public PlaybackOrder getPlaybackOrder () {
		return this.playbackOrder;
	}

	public Transcode getTranscode () {
		return this.transcode;
	}

	public PlayItem getCurrentItem () {
		return this.currentItem;
	}

	public long getPosition () {
		return this.position;
	}

	public boolean isPlaying () {
		return this.isPlaying;
	}

	public void addItemsToQueue (final PlayerQueue queue) {
		for (final PlayItem item : this.queueItems) {
			queue.addToQueue(item.withoutId());
		}
	}

	@Override
	public String toString () {
		return String.format("PlayerState{po=%s ci=%s p=%ss i=%s q=%s}",
				this.playbackOrder, this.currentItem, this.position, this.isPlaying, this.queueItems.size());
	}

}
