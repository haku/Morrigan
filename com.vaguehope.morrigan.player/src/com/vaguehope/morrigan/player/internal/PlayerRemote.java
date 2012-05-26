package com.vaguehope.morrigan.player.internal;

import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.RemotePlayer;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;

/**
 * TODO rename to RemotePlayerImpl
 */
public class PlayerRemote implements RemotePlayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final int id;
	private final String name;
	private final String remoteHost;
	private final int remotePlayerId;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public PlayerRemote (int id, String name, String remoteHost, int remotePlayerId) {
		this.id = id;
		this.name = name;
		this.remoteHost = remoteHost;
		this.remotePlayerId = remotePlayerId;

	}
	@Override
	public void dispose() {
		// Unused.
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getRemoteHost() {
		return this.remoteHost;
	}

	@Override
	public int getRemotePlayerId() {
		return this.remotePlayerId;
	}

	@Override
	public boolean isAvailable() {
		throw new RuntimeException("Not implemented.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean isPlaybackEngineReady() {
		return false;
	}

	@Override
	public void loadAndStartPlaying(IMediaTrackList<? extends IMediaTrack> list) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void loadAndStartPlaying(IMediaTrackList<? extends IMediaTrack> list, IMediaTrack track) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void loadAndStartPlaying(PlayItem item) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void pausePlaying() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void stopPlaying() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void nextTrack() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public PlayState getPlayState() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public PlayItem getCurrentItem() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public IMediaTrackList<? extends IMediaTrack> getCurrentList() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public long getCurrentPosition() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public int getCurrentTrackDuration() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void seekTo(double d) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public PlaybackOrder getPlaybackOrder() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void setPlaybackOrder(PlaybackOrder order) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public List<PlayItem> getHistory() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void addToQueue(PlayItem item) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void addToQueue(List<PlayItem> item) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void removeFromQueue(PlayItem item) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void clearQueue() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void moveInQueue(List<PlayItem> items, boolean moveDown) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void moveInQueueEnd(List<PlayItem> items, boolean toBottom) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void shuffleQueue() {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public List<PlayItem> getQueueList() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void setQueueList(List<PlayItem> items) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public DurationData getQueueTotalDuration() {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public PlayItem getQueueItemById(int itemId) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Map<Integer, String> getMonitors() {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void goFullscreen(int monitor) {
		throw new UnsupportedOperationException("Not implemented.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
