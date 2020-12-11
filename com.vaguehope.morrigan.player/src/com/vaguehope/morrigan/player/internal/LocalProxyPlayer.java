package com.vaguehope.morrigan.player.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.LocalPlayerSupport;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerQueue;
import com.vaguehope.morrigan.transcode.Transcode;

/**
 * FIXME
 * Currently this impl polls the underlying impl every second to generate change events.
 * This could be made much more efficient.
 */
public class LocalProxyPlayer implements LocalPlayer {

	private final String refId;
	private final String refName;
	private final AtomicReference<Player> ref = new AtomicReference<Player>();
	private final LocalPlayerSupport localPlayerSupport;

	public LocalProxyPlayer (final Player player, final LocalPlayerSupport eventHandler) {
		if (player == null) throw new IllegalArgumentException("Player can not be null.");
		this.refId = player.getId();
		this.refName = player.getName();
		this.ref.set(player);
		this.localPlayerSupport = eventHandler;
		this.localPlayerSupport.historyChanged(); // undefined --> empty list.
	}

	@Override
	public void dispose () {
		this.ref.set(null);
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
	public String getId () {
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
	public void addEventListener (final PlayerEventListener listener) {
		final Player p = getRef();
		if (p == null) return;
		p.addEventListener(listener);
	}

	@Override
	public void removeEventListener (final PlayerEventListener listener) {
		final Player p = getRef();
		if (p == null) return;
		p.removeEventListener(listener);
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
	public void setCurrentItem (final PlayItem item) {
		final Player p = getRef();
		if (p == null) return;
		p.setCurrentItem(item);
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
		if (p == null) return -1;
		return p.getCurrentTrackDuration();
	}

	@Override
	public int getCurrentTrackDurationAsMeasured () {
		final Player p = getRef();
		if (p == null) return -1;
		return p.getCurrentTrackDurationAsMeasured();
	}

	@Override
	public int getCurrentTrackDurationFromRenderer () {
		final Player p = getRef();
		if (p == null) return -1;
		return p.getCurrentTrackDurationFromRenderer();
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
	public Integer getVoume() {
		final Player p = getRef();
		if (p == null) return null;
		return p.getVoume();
	}

	@Override
	public Integer getVoumeMaxValue() {
		final Player p = getRef();
		if (p == null) return null;
		return p.getVoumeMaxValue();
	}

	@Override
	public void setVolume(final int newVolume) {
		final Player p = getRef();
		if (p == null) return;
		p.setVolume(newVolume);
	}

	@Override
	public Transcode getTranscode () {
		final Player p = getRef();
		if (p == null) return null;
		return p.getTranscode();
	}

	@Override
	public void setTranscode (final Transcode transcode) {
		final Player p = getRef();
		if (p == null) return;
		p.setTranscode(transcode);
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
		if (p == null) return ImmutableEmptyPlayerQueue.INSTANCE;
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
