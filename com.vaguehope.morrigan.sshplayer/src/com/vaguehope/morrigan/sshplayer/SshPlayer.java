package com.vaguehope.morrigan.sshplayer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.IPlayerAbstract;
import com.vaguehope.morrigan.player.OrderHelper;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;

public class SshPlayer implements IPlayerAbstract {

	private static final Logger LOG = Logger.getLogger(SshPlayer.class.getName());

	private final int playerId;

	private PlaybackOrder _playbackOrder = PlaybackOrder.SEQUENTIAL;

	public SshPlayer (int id) {
		this.playerId = id;
	}

	@Override
	public int getId () {
		return this.playerId;
	}

	@Override
	public void dispose () {
		// Unused.
	}

	@Override
	public String getName () {
		return "My ssh player";
	}

	@Override
	public boolean isPlaybackEngineReady () {
		return true;
	}

	@Override
	public void loadAndStartPlaying (IMediaTrackList<? extends IMediaTrack> list) {
		IMediaTrack nextTrack = OrderHelper.getNextTrack(list, null, this._playbackOrder);
		loadAndStartPlaying(list, nextTrack);
	}

	@Override
	public void loadAndStartPlaying (IMediaTrackList<? extends IMediaTrack> list, IMediaTrack track) {
		if (track == null) throw new NullPointerException();
		loadAndStartPlaying(new PlayItem(list, track));
	}

	@Override
	public void loadAndStartPlaying (PlayItem item) {
		LOG.info("TODO: load and start playing: " + item);
	}

	@Override
	public void pausePlaying () {
		LOG.info("TODO: pause");
	}

	@Override
	public void stopPlaying () {
		LOG.info("TODO: stop");
	}

	@Override
	public void nextTrack () {
		LOG.info("TODO: next");
	}

	@Override
	public PlayState getPlayState () {
		return PlayState.Stopped;
	}

	@Override
	public PlayItem getCurrentItem () {
		return null;
	}

	@Override
	public IMediaTrackList<? extends IMediaTrack> getCurrentList () {
		return null;
	}

	@Override
	public long getCurrentPosition () {
		return 0;
	}

	@Override
	public int getCurrentTrackDuration () {
		return 1;
	}

	@Override
	public void seekTo (double d) {
		LOG.info("TODO: seek");
	}

	@Override
	public PlaybackOrder getPlaybackOrder () {
		return this._playbackOrder;
	}

	@Override
	public void setPlaybackOrder (PlaybackOrder order) {
		this._playbackOrder = order;
	}

	@Override
	public List<PlayItem> getHistory () {
		return Collections.emptyList();
	}

	@Override
	public void addToQueue (PlayItem item) {
		LOG.info("TODO: add to queue: " + item);
	}

	@Override
	public void addToQueue (List<PlayItem> items) {
		LOG.info("TODO: add to queue: " + items);
	}

	@Override
	public void removeFromQueue (PlayItem item) {
		LOG.info("TODO: remove from queue: " + item);
	}

	@Override
	public void clearQueue () {
		LOG.info("TODO: clear queue");
	}

	@Override
	public void moveInQueue (List<PlayItem> items, boolean moveDown) {
		LOG.info("TODO: move in queue");
	}

	@Override
	public void moveInQueueEnd (List<PlayItem> items, boolean toBottom) {
		LOG.info("TODO: move to end of queue");
	}

	@Override
	public List<PlayItem> getQueueList () {
		return Collections.emptyList();
	}

	@Override
	public void setQueueList (List<PlayItem> items) {
		LOG.info("TODO: set queue list");
	}

	@Override
	public void shuffleQueue () {
		LOG.info("TODO: shuffle queue");
	}

	@Override
	public DurationData getQueueTotalDuration () {
		return new DurationData() {
			@Override
			public boolean isComplete () {
				return true;
			}
			@Override
			public long getDuration () {
				return 0;
			}
		};
	}

	@Override
	public PlayItem getQueueItemById (int id) {
		return null;
	}

	@Override
	public Map<Integer, String> getMonitors () {
		return Collections.emptyMap();
	}

	@Override
	public void goFullscreen (int monitor) {
		LOG.info("TODO: todo go full screen?");
	}

}
