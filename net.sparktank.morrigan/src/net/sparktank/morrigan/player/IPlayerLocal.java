package net.sparktank.morrigan.player;

import java.util.List;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.media.impl.DurationData;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrackList;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;

import org.eclipse.swt.widgets.Composite;

public interface IPlayerLocal {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void dispose();
	
	public int getId();
	
	public PlayItem getCurrentItem();
	public IMediaTrackList<? extends IMediaTrack> getCurrentList();
	
	public PlaybackOrder getPlaybackOrder();
	public void setPlaybackOrder(PlaybackOrder order);
	
	public List<PlayItem> getHistory();
	
	public void addToQueue(PlayItem item);
	public void removeFromQueue(PlayItem item);
	public void clearQueue();
	public void moveInQueue(List<PlayItem> items, boolean moveDown);
	public List<PlayItem> getQueueList();
	public DurationData getQueueTotalDuration();
	public void addQueueChangeListener(Runnable listener);
	public void removeQueueChangeListener(Runnable listener);
	
	public boolean isPlaybackEngineReady();
	public void loadAndStartPlaying(MediaExplorerItem item) throws MorriganException;
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying(IMediaTrackList<? extends IMediaTrack> list);
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying(IMediaTrackList<? extends IMediaTrack> list, IMediaTrack track);
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying(final PlayItem item);
	/**
	 * For UI handlers to call.
	 */
	public void pausePlaying();
	/**
	 * For UI handlers to call.
	 */
	public void stopPlaying();
	public void nextTrack();
	public PlayState getPlayState();
	
	public long getCurrentPosition();
	public int getCurrentTrackDuration();
	public void seekTo(double d);
	
	public void setVideoFrameParent(Composite cmfp);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}