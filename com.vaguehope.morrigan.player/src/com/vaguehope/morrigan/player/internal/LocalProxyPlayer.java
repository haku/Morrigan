package com.vaguehope.morrigan.player.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerEventHandler;
import com.vaguehope.morrigan.player.PlayerQueue;

/**
 * FIXME
 * Currently this impl polls the underlying impl every second to generate change events.
 * This could be made much more efficient.
 */
public class LocalProxyPlayer implements LocalPlayer {

	private final int refId;
	private final String refName;
	private final AtomicReference<Player> ref = new AtomicReference<Player>();
	private final PlayerEventHandler eventHandler;
	private final ScheduledFuture<?> scheduledFuture;

	public LocalProxyPlayer (final Player player, final PlayerEventHandler eventHandler, final ScheduledExecutorService scheduledExecutorService) {
		if (player == null) throw new IllegalArgumentException("Player can not be null.");
		this.refId = player.getId();
		this.refName = player.getName();
		this.ref.set(player);
		this.eventHandler = eventHandler;
		this.scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
			private PlayItem lastItem;

			@Override
			public void run () {
				final PlayItem currentItem = getCurrentItem();
				if (this.lastItem != currentItem) {
					this.lastItem = currentItem;
					eventHandler.currentItemChanged();
				}
				eventHandler.updateStatus(); // TODO Player needs addListener() and removeListener().
			}
		}, 1, 1, TimeUnit.SECONDS);
		this.eventHandler.historyChanged(); // undefined --> empty list.
	}

	@Override
	public void dispose () {
		if (this.ref.getAndSet(null) != null) {
			this.scheduledFuture.cancel(false);
		}
	}

	private Player getRef () {
		final Player player = this.ref.get();
		if (player == null) return null;
		if (player.isDisposed()) {
			dispose();
			return null;
		}
		return player;
	}

	@Override
	public String toString () {
		return new StringBuilder("LocalProxyPlayer{")
				.append(this.refId)
				.append(", ").append(this.refName)
				.append("}").toString();
	}

	@Override
	public int getId () {
		return this.refId;
	}

	@Override
	public String getName () {
		final Player p = getRef();
		if (p == null) return "(dead proxy for " + this.refName + ")";
		return p.getName();
	}

	@Override
	public boolean isProxy () {
		return true;
	}

	@Override
	public boolean isDisposed () {
		final Player p = getRef();
		if (p == null) return false;
		return p.isDisposed();
	}

	@Override
	public boolean isPlaybackEngineReady () {
		final Player p = getRef();
		if (p == null) return false;
		return p.isPlaybackEngineReady();
	}

	@Override
	public void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list) {
		final Player p = getRef();
		if (p == null) return;
		p.loadAndStartPlaying(list);
	}

	@Override
	public void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack track) {
		final Player p = getRef();
		if (p == null) return;
		p.loadAndStartPlaying(list, track);
	}

	@Override
	public void loadAndStartPlaying (final PlayItem item) {
		final Player p = getRef();
		if (p == null) return;
		p.loadAndStartPlaying(item);
	}

	@Override
	public void pausePlaying () {
		final Player p = getRef();
		if (p == null) return;
		p.pausePlaying();
	}

	@Override
	public void stopPlaying () {
		final Player p = getRef();
		if (p == null) return;
		p.stopPlaying();
	}

	@Override
	public void nextTrack () {
		final Player p = getRef();
		if (p == null) return;
		p.nextTrack();
	}

	@Override
	public PlayState getPlayState () {
		final Player p = getRef();
		if (p == null) return PlayState.STOPPED;
		return p.getPlayState();
	}

	@Override
	public PlayItem getCurrentItem () {
		final Player p = getRef();
		if (p == null) return null;
		return p.getCurrentItem();
	}

	@Override
	public IMediaTrackList<? extends IMediaTrack> getCurrentList () {
		final Player p = getRef();
		if (p == null) return null;
		return p.getCurrentList();
	}

	@Override
	public long getCurrentPosition () {
		final Player p = getRef();
		if (p == null) return -1;
		return p.getCurrentPosition();
	}

	@Override
	public int getCurrentTrackDuration () {
		final Player p = getRef();
		if (p == null) return 0;
		return p.getCurrentTrackDuration();
	}

	@Override
	public void seekTo (final double d) {
		final Player p = getRef();
		if (p == null) return;
		p.seekTo(d);
	}

	@Override
	public PlaybackOrder getPlaybackOrder () {
		final Player p = getRef();
		if (p == null) return null;
		return p.getPlaybackOrder();
	}

	@Override
	public void setPlaybackOrder (final PlaybackOrder order) {
		final Player p = getRef();
		if (p == null) return;
		p.setPlaybackOrder(order);
	}

	@Override
	public List<PlayItem> getHistory () {
		final Player p = getRef();
		if (p == null) return Collections.emptyList();
		return p.getHistory();
	}

	@Override
	public PlayerQueue getQueue () {
		final Player p = getRef();
		if (p == null) return null;
		return p.getQueue();
	}

	@Override
	public Map<Integer, String> getMonitors () {
		final Player p = getRef();
		if (p == null) return Collections.emptyMap();
		return p.getMonitors();
	}

	@Override
	public void goFullscreen (final int monitor) {
		final Player p = getRef();
		if (p == null) return;
		p.goFullscreen(monitor);
	}

	@Override
	public void setVideoFrameParent (final Composite cmfp) {
		// NoOp.
	}

}
