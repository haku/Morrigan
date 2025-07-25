package com.vaguehope.morrigan.player;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.transcode.Transcode;

public interface Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void dispose();
	boolean isDisposed();
	void addOnDisposeListener(Runnable runnable);

	String getId();
	String getName();

	/**
	 * If this returns true then ok to call setVideoFrameParent();
	 */
	boolean isPlaybackEngineReady();

	/**
	 * May be async.
	 */
	void loadAndStartPlaying(MediaList list);

	/**
	 * May be async.
	 */
	void loadAndStartPlaying(MediaList list, MediaItem track, final long playFromPositionMillis);

	/**
	 * May be async.
	 */
	void loadAndStartPlaying(final PlayItem item, final long playFromPositionMillis, final boolean startPaused);

	void pausePlaying();

	void stopPlaying();

	/**
	 * May be async.
	 */
	void nextTrack();

	PlayState getPlayState();

	// may return previous item.
	PlayItem setCurrentItem(PlayItem item);
	PlayItem getCurrentItem();

	void setCurrentList(MediaList list);
	MediaList getCurrentList();

	/**
	 * Return -1 if not playing.
	 * In seconds.
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
	@Deprecated
	void seekTo(double d);

	void setPositionMillis(final long millis);

	PlaybackOrder getPlaybackOrder();
	PlaybackOrder getPlaybackOrderOverride();
	void setPlaybackOrder(PlaybackOrder order);

	/**
	 * Returns null if not implemented.
	 */
	Integer getVolume();
	Integer getVolumeMaxValue();
	void setVolume(int newVolume);

	/**
	 * Can not be null;
	 */
	Transcode getTranscode();
	/**
	 * Can not be null;
	 */
	void setTranscode(Transcode transcode);

	PlayerQueue getQueue();

	void markStateRestoreAttempted();

	public interface PlayerEventListener {

		void playOrderChanged (PlaybackOrder newPlaybackOrder);
		void transcodeChanged (Transcode newTranscode);
		/**
		 * Either the item is now a different item, or a property on the item has changed.
		 * May be called with null to indicate no item or an unknown item.
		 */
		void currentItemChanged (PlayItem newItem);
		void playStateChanged(PlayState newPlayState, PlayItem newItem);
		/**
		 * Called when either position or duration change.
		 * Both in seconds.
		 */
		void positionChanged(long newPosition, int duration);

		/**
		 * Called after successful seekTo() invocation.
		 */
		void afterSeek();

		/**
		 * Called when errors occur during play back.
		 */
		void onException (Exception e);

	}

	void addEventListener(PlayerEventListener listener);
	void removeEventListener(PlayerEventListener listener);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
