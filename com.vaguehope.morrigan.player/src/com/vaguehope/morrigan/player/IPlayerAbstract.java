package com.vaguehope.morrigan.player;

import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;


public interface IPlayerAbstract {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void dispose();
	
	public int getId();
	public String getName();
	
	public boolean isPlaybackEngineReady();
	public void loadAndStartPlaying(IMediaTrackList<? extends IMediaTrack> list);
	public void loadAndStartPlaying(IMediaTrackList<? extends IMediaTrack> list, IMediaTrack track);
	public void loadAndStartPlaying(final PlayItem item);
	public void pausePlaying();
	public void stopPlaying();
	public void nextTrack();
	public PlayState getPlayState();
	
	public PlayItem getCurrentItem();
	public IMediaTrackList<? extends IMediaTrack> getCurrentList();
	
	public long getCurrentPosition();
	public int getCurrentTrackDuration();
	public void seekTo(double d);
	
	public PlaybackOrder getPlaybackOrder();
	public void setPlaybackOrder(PlaybackOrder order);
	
	public List<PlayItem> getHistory();
	
	public void addToQueue(PlayItem item);
	public void addToQueue(List<PlayItem> item);
	public void removeFromQueue(PlayItem item);
	public void clearQueue();
	public void moveInQueue(List<PlayItem> items, boolean moveDown);
	public void moveInQueueEnd(List<PlayItem> items, boolean toBottom);
	public List<PlayItem> getQueueList();
	public void setQueueList (List<PlayItem> items);
	public void shuffleQueue ();
	public DurationData getQueueTotalDuration();
	public PlayItem getQueueItemById (int id);
	
	public Map<Integer, String> getMonitors ();
	public void goFullscreen (int monitor);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
