package com.vaguehope.morrigan.sshplayer;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.Register;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.DefaultPlayerQueue;
import com.vaguehope.morrigan.player.OrderHelper;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerQueue;

public class SshPlayer implements Player {

	private static final Logger LOG = Logger.getLogger(SshPlayer.class.getName());

	private final int playerId;
	private final CliHost host;
	private final Register<Player> register;
	private final DefaultPlayerQueue queue;

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final AtomicReference<PlaybackOrder> playbackOrder = new AtomicReference<PlaybackOrder>(PlaybackOrder.SEQUENTIAL);
	private final AtomicReference<CliPlayer> cliPlayer = new AtomicReference<CliPlayer>();
	private final AtomicReference<PlayItem> currentItem = new AtomicReference<PlayItem>();

	public SshPlayer (final int id, final CliHost host, final Register<Player> register) {
		this.playerId = id;
		this.host = host;
		this.register = register;
		this.queue = new DefaultPlayerQueue();
	}

	@Override
	public int getId () {
		return this.playerId;
	}

	@Override
	public void dispose () {
		if (this.alive.compareAndSet(true, false)) {
			this.register.unregister(this);
		}
	}

	@Override
	public boolean isDisposed () {
		return !this.alive.get();
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
	public void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list) {
		IMediaTrack nextTrack = OrderHelper.getNextTrack(list, null, this.playbackOrder.get());
		loadAndStartPlaying(list, nextTrack);
	}

	@Override
	public void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack track) {
		if (track == null) throw new NullPointerException();
		loadAndStartPlaying(new PlayItem(list, track));
	}

	@Override
	public void loadAndStartPlaying (final PlayItem item) {
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
		if (m == null) return PlayState.STOPPED;
		// TODO what about paused?
		return m.isRunning() ? PlayState.PLAYING : PlayState.STOPPED;
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
	public void seekTo (final double d) {
		LOG.info("TODO: seek: " + d);
	}

	@Override
	public PlaybackOrder getPlaybackOrder () {
		return this.playbackOrder.get();
	}

	@Override
	public void setPlaybackOrder (final PlaybackOrder order) {
		this.playbackOrder.set(order);
	}

	@Override
	public List<PlayItem> getHistory () {
		return Collections.emptyList();
	}

	@Override
	public PlayerQueue getQueue () {
		return this.queue;
	}

	@Override
	public Map<Integer, String> getMonitors () {
		return Collections.emptyMap();
	}

	@Override
	public void goFullscreen (final int monitor) {
		LOG.info("TODO: todo go full screen?");
	}

}
