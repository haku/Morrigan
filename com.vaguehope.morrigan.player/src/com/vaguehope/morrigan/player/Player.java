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
	boolean isDisposed();

	int getId();
	String getName();

	/**
	 * If this returns true then ok to call setVideoFrameParent();
	 */
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

	/**
	 * Return -1 if not playing.
	 */
	long getCurrentPosition();

	/**
	 * Return -1 if not playing.
	 */
	int getCurrentTrackDuration();

	void seekTo(double d);

	PlaybackOrder getPlaybackOrder();
	void setPlaybackOrder(PlaybackOrder order);

	List<PlayItem> getHistory();
	PlayerQueue getQueue();

	Map<Integer, String> getMonitors ();
	void goFullscreen (int monitor);

	public interface PlayerEventListener {

		void playOrderChanged (PlaybackOrder newPlaybackOrder);
		/**
		 * Either the item is now a different item, or a property on the item has changed.
		 * May be called with null to indicate no item or an unknown item.
		 */
		void currentItemChanged (PlayItem newItem);
		void playStateChanged(PlayState newPlayState);
		/**
		 * Called when either position or duration change.
		 * Both in seconds.
		 */
		void positionChanged(long newPosition, int duration);

		/**
		 * Called when errors occur during play back.
		 */
		void onException (Exception e);

	}

	void addEventListener(PlayerEventListener listener);
	void removeEventListener(PlayerEventListener listener);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
