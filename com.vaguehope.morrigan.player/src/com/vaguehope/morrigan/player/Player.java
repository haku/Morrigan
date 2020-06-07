package com.vaguehope.morrigan.player;

import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.transcode.Transcode;

public interface Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void dispose();
	boolean isDisposed();

	String getId();
	String getName();

	/**
	 * If this returns true then ok to call setVideoFrameParent();
	 */
	boolean isPlaybackEngineReady();

	/**
	 * May be async.
	 */
	void loadAndStartPlaying(IMediaTrackList<? extends IMediaTrack> list);

	/**
	 * May be async.
	 */
	void loadAndStartPlaying(IMediaTrackList<? extends IMediaTrack> list, IMediaTrack track);

	/**
	 * May be async.
	 */
	void loadAndStartPlaying(final PlayItem item);

	void pausePlaying();

	void stopPlaying();

	/**
	 * May be async.
	 */
	void nextTrack();

	PlayState getPlayState();

	void setCurrentItem(PlayItem item);
	PlayItem getCurrentItem();
	IMediaTrackList<? extends IMediaTrack> getCurrentList();

	/**
	 * Return -1 if not playing.
	 */
	long getCurrentPosition();

	/**
	 * Return -1 if not playing.
	 * In seconds.
	 */
	int getCurrentTrackDurationFromRenderer();

	/**
	 * This will override all other sources of duration info.
	 * Return -1 if not known.
	 * In seconds.
	 */
	int getCurrentTrackDurationAsMeasured();

	/**
	 * Tries to read duration from current item first, falling back to that reported by renderer.
	 * Returns <0 if not available.
	 * In seconds.
	 */
	int getCurrentTrackDuration();

	/**
	 * Takes a percentage of duration.
	 * @param d a double where 0 <= d <= 1.
	 */
	void seekTo(double d);

	PlaybackOrder getPlaybackOrder();
	void setPlaybackOrder(PlaybackOrder order);

	/**
	 * Can not be null;
	 */
	Transcode getTranscode();
	/**
	 * Can not be null;
	 */
	void setTranscode(Transcode transcode);

	List<PlayItem> getHistory();
	PlayerQueue getQueue();

	Map<Integer, String> getMonitors ();
	void goFullscreen (int monitor);

	public interface PlayerEventListener {

		void playOrderChanged (PlaybackOrder newPlaybackOrder);
		void transcodeChanged (Transcode newTranscode);
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
