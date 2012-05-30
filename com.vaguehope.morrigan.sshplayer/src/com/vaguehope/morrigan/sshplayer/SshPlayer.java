package com.vaguehope.morrigan.sshplayer;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.Register;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.OrderHelper;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;

public class SshPlayer implements Player {

	private static final Logger LOG = Logger.getLogger(SshPlayer.class.getName());

	private final int playerId;
	private final CliHost host;
	private final Register<Player> register;

	private AtomicReference<PlaybackOrder> playbackOrder = new AtomicReference<PlaybackOrder>(PlaybackOrder.SEQUENTIAL);
	private AtomicReference<CliPlayer> cliPlayer = new AtomicReference<CliPlayer>();
	private AtomicReference<PlayItem> currentItem = new AtomicReference<PlayItem>();

	public SshPlayer (int id, CliHost host, Register<Player> register) {
		this.playerId = id;
		this.host = host;
		this.register = register;
	}

	@Override
	public int getId () {
		return this.playerId;
	}

	@Override
	public void dispose () {
		this.register.unregister(this);
	}

	@Override
	public String getName () {
		return "ssh:" + this.host.getName();
	}

	@Override
	public boolean isPlaybackEngineReady () {
		return true;
	}

	@Override
	public void loadAndStartPlaying (IMediaTrackList<? extends IMediaTrack> list) {
		IMediaTrack nextTrack = OrderHelper.getNextTrack(list, null, this.playbackOrder.get());
		loadAndStartPlaying(list, nextTrack);
	}

	@Override
	public void loadAndStartPlaying (IMediaTrackList<? extends IMediaTrack> list, IMediaTrack track) {
		if (track == null) throw new NullPointerException();
		loadAndStartPlaying(new PlayItem(list, track));
	}

	@Override
	public void loadAndStartPlaying (PlayItem item) {
		File media = new File(item.item.getFilepath());
		if (!media.exists()) {
			// TODO report to some status line.
			LOG.warning("File not found: " + media.getAbsolutePath());
			return;
		}
		LOG.info("Loading item: " + media.getAbsolutePath());

		stopPlaying();
		CliPlayer newMp = new CliPlayer(this.host, media);
		if (!this.cliPlayer.compareAndSet(null, newMp)) {
			LOG.warning("Another thread set the player.  Aborting playback of: " + item);
			return;
		}

		newMp.start();
		this.currentItem.set(item);
	}

	@Override
	public void pausePlaying () {
		CliPlayer m = this.cliPlayer.get();
		if (m != null) m.togglePaused();
	}

	@Override
	public void stopPlaying () {
		CliPlayer mp = this.cliPlayer.getAndSet(null);
		if (mp != null) {
			try {
				mp.cancel();
			}
			catch (InterruptedException e) {
				LOG.log(Level.WARNING, "Interupted while waiting for playback to stop.", e);
			}
			finally {
				this.currentItem.set(null);
			}
		}
	}

	@Override
	public void nextTrack () {
		LOG.info("TODO: next");
	}

	@Override
	public PlayState getPlayState () {
		CliPlayer m = this.cliPlayer.get();
		if (m == null) return PlayState.Stopped;
		// TODO what about paused?
		return m.isRunning() ? PlayState.Playing : PlayState.Stopped;
	}

	@Override
	public PlayItem getCurrentItem () {
		return this.currentItem.get();
	}

	@Override
	public IMediaTrackList<? extends IMediaTrack> getCurrentList () {
		PlayItem item = this.currentItem.get();
		return item == null ? null : item.list;
	}

	@Override
	public long getCurrentPosition () {
		CliPlayer m = this.cliPlayer.get();
		return m == null ? -1 : m.getCurrentPosition();
	}

	@Override
	public int getCurrentTrackDuration () {
		CliPlayer m = this.cliPlayer.get();
		return m == null ? -1 : m.getDuration();
	}

	@Override
	public void seekTo (double d) {
		LOG.info("TODO: seek: " + d);
	}

	@Override
	public PlaybackOrder getPlaybackOrder () {
		return this.playbackOrder.get();
	}

	@Override
	public void setPlaybackOrder (PlaybackOrder order) {
		this.playbackOrder.set(order);
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
