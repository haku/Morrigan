package com.vaguehope.morrigan.player;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.player.Player.PlayerEventListener;
import com.vaguehope.morrigan.transcode.Transcode;

public class PlayerEventListenerCaller implements PlayerEventListener {

	private static final Logger LOG = LoggerFactory.getLogger(PlayerEventListenerCaller.class);

	private final Set<PlayerEventListener> watchers = Collections.newSetFromMap(new ConcurrentHashMap<PlayerEventListener, Boolean>());

	public void addEventListener(final PlayerEventListener listener) {
		this.watchers.add(listener);
	}

	public void removeEventListener(final PlayerEventListener listener) {
		this.watchers.remove(listener);
	}

	private static void logException(final Exception e) {
		LOG.warn("Exception from listener:", e);
	}

	@Override
	public void currentItemChanged(final PlayItem newItem) {
		for (final PlayerEventListener l : this.watchers) {
			try {
				l.currentItemChanged(newItem);
			}
			catch (final Exception e) {
				logException(e);
			}
		}
	}

	@Override
	public void playStateChanged(final PlayState newPlayState, final PlayItem newItem) {
		for (final PlayerEventListener l : this.watchers) {
			try {
				l.playStateChanged(newPlayState, newItem);
			}
			catch (final Exception e) {
				logException(e);
			}
		}
	}

	@Override
	public void positionChanged(final long newPosition, final int duration) {
		for (final PlayerEventListener l : this.watchers) {
			try {
				l.positionChanged(newPosition, duration);
			}
			catch (final Exception e) {
				logException(e);
			}
		}
	}

	@Override
	public void afterSeek() {
		for (final PlayerEventListener l : this.watchers) {
			try {
				l.afterSeek();
			}
			catch (final Exception e) {
				logException(e);
			}
		}
	}

	@Override
	public void playOrderChanged(final PlaybackOrder newPlaybackOrder) {
		for (final PlayerEventListener l : this.watchers) {
			try {
				l.playOrderChanged(newPlaybackOrder);
			}
			catch (final Exception e) {
				logException(e);
			}
		}
	}

	@Override
	public void transcodeChanged(final Transcode newTranscode) {
		for (final PlayerEventListener l : this.watchers) {
			try {
				l.transcodeChanged(newTranscode);
			}
			catch (final Exception e) {
				logException(e);
			}
		}
	}

	@Override
	public void onException(final Exception ex) {
		for (final PlayerEventListener l : this.watchers) {
			try {
				l.onException(ex);
			}
			catch (final Exception e) {
				logException(e);
			}
		}
	}

}
