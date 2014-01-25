package com.vaguehope.morrigan.player;

import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;

public interface Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void dispose();

	int getId();
	String getName();

	boolean isPlaybackEngineReady();
	void loadAndStartPlaying(IMediaTrackList<? extends IMediaTrack> list);
	void loadAndStartPlaying(IMediaTrackList<? extends IMediaTrack> list, IMediaTrack track);
	void loadAndStartPlaying(final PlayItem item);
	void pausePlaying();
	void stopPlaying();
	void nextTrack();
	PlayState getPlayState();

	PlayItem getCurrentItem();
	IMediaTrackList<? extends IMediaTrack> getCurrentList();

	long getCurrentPosition();
	int getCurrentTrackDuration();
	void seekTo(double d);

	PlaybackOrder getPlaybackOrder();
	void setPlaybackOrder(PlaybackOrder order);

	List<PlayItem> getHistory();
	PlayerQueue getQueue();

	Map<Integer, String> getMonitors ();
	void goFullscreen (int monitor);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
