package com.vaguehope.morrigan.player;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.Player.PlayerEventListener;

public class PlayerEventListenerCaller implements PlayerEventListener {

	private final Set<PlayerEventListener> watchers = Collections.newSetFromMap(new ConcurrentHashMap<PlayerEventListener, Boolean>());

	public void addEventListener (final PlayerEventListener listener) {
		this.watchers.add(listener);
	}

	public void removeEventListener (final PlayerEventListener listener) {
		this.watchers.remove(listener);
	}

	@Override
	public void currentItemChanged (final PlayItem newItem) {
		for (final PlayerEventListener l : this.watchers) {
			l.currentItemChanged(newItem);
		}
	}

	@Override
	public void playStateChanged (final PlayState newPlayState) {
		for (final PlayerEventListener l : this.watchers) {
			l.playStateChanged(newPlayState);
		}
	}

	@Override
	public void positionChanged (final long newPosition, final int duration) {
		for (final PlayerEventListener l : this.watchers) {
			l.positionChanged(newPosition, duration);
		}
	}

	@Override
	public void playOrderChanged (final PlaybackOrder newPlaybackOrder) {
		for (final PlayerEventListener l : this.watchers) {
			l.playOrderChanged(newPlaybackOrder);
		}
	}

}
