package com.vaguehope.morrigan.player.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerQueue;
import com.vaguehope.morrigan.player.RemotePlayer;

public class RemotePlayerImpl implements RemotePlayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final int id;
	private final String name;
	private final String remoteHost;
	private final int remotePlayerId;
	private final AtomicBoolean alive = new AtomicBoolean(true);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public RemotePlayerImpl (final int id, final String name, final String remoteHost, final int remotePlayerId) {
		this.id = id;
		this.name = name;
		this.remoteHost = remoteHost;
		this.remotePlayerId = remotePlayerId;

	}

	@Override
	public void dispose () {
		this.alive.set(false);
	}

	@Override
	public boolean isDisposed () {
		return !this.alive.get();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public int getId () {
		return this.id;
	}

	@Override
	public String getName () {
		return this.name;
	}

	@Override
	public String getRemoteHost () {
		return this.remoteHost;
	}

	@Override
	public int getRemotePlayerId () {
		return this.remotePlayerId;
	}

	@Override
	public boolean isAvailable () {
		throw new UnsupportedOperationException("Not implemented.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean isPlaybackEngineReady () {
		return false;
	}

	@Override
	public void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void loadAndStartPlaying (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack track) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void loadAndStartPlaying (final PlayItem item) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void pausePlaying () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void stopPlaying () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void nextTrack () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public PlayState getPlayState () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public PlayItem getCurrentItem () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public IMediaTrackList<? extends IMediaTrack> getCurrentList () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public long getCurrentPosition () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public int getCurrentTrackDuration () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void seekTo (final double d) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public PlaybackOrder getPlaybackOrder () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setPlaybackOrder (final PlaybackOrder order) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public List<PlayItem> getHistory () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public PlayerQueue getQueue () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Map<Integer, String> getMonitors () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void goFullscreen (final int monitor) {
		throw new UnsupportedOperationException("Not implemented.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
