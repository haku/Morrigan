package com.vaguehope.morrigan.sshplayer;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.AbstractPlayer;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;

public class SshPlayer extends AbstractPlayer {

	private static final Logger LOG = Logger.getLogger(SshPlayer.class.getName());

	private final CliHost host;
	private final AtomicReference<CliPlayer> cliPlayer = new AtomicReference<CliPlayer>();
	private final AtomicReference<PlayItem> currentItem = new AtomicReference<PlayItem>();

	public SshPlayer (final int id, final CliHost host, final PlayerRegister register) {
		super(id, "ssh:" + host.getName(), register);
		this.host = host;
	}

	@Override
	protected void onDispose () {
		System.err.println("Disposed player: " + toString());
	}

	@Override
	public boolean isPlaybackEngineReady () {
		return true;
	}

	@Override
	protected void loadAndStartPlaying (final PlayItem item, final File file) throws Exception {
		LOG.info("Loading item: " + file.getAbsolutePath());

		stopPlaying();
		final CliPlayer newMp = new CliPlayer(this.host, file);
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
		return item == null ? null : item.getList();
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
	public List<PlayItem> getHistory () {
		return Collections.emptyList();
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
