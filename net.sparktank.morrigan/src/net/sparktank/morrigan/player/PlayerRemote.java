package net.sparktank.morrigan.player;

import java.util.List;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.media.impl.DurationData;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrackList;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;

public class PlayerRemote implements IPlayerRemote {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final int id;
	private final String remoteHost;
	private final int remotePlayerId;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PlayerRemote (int id, String remoteHost, int remotePlayerId) {
		this.id = id;
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
	public void loadAndStartPlaying(MediaExplorerItem item) throws MorriganException {
		throw new RuntimeException("Not implemented.");
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
	public List<PlayItem> getQueueList() {
		throw new RuntimeException("Not implemented.");
	}
	
	@Override
	public DurationData getQueueTotalDuration() {
		throw new RuntimeException("Not implemented.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
