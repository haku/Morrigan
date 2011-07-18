package net.sparktank.morrigan.playbackimpl.gs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;

/**
 * TODO remove need to pass in entire PlaybackEngine.
 */
public class WatcherThread extends Thread {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	final private AtomicBoolean watchThreadStop = new AtomicBoolean(false);
	
	private int lastPositionSec = -1;
	private int lastDurationSec = -1;
	private int eosManCounter = 0;

	private final PlaybackEngine engine;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public WatcherThread (PlaybackEngine engine) {
		this.engine = engine;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void stopWatching () {
		this.watchThreadStop.set(true);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run () {
		while (!this.watchThreadStop.get()) {
			process();
			
			try {
				Thread.sleep(Constants.WATCHER_POLL_INTERVAL_MILLIS);
			} catch (InterruptedException e) { /* UNUSED */ }
		}
	}
	
	private void process () {
		if (this.engine.playbin != null) {
			int positionSec = (int) this.engine.playbin.queryPosition(TimeUnit.SECONDS);
			int durationSec = (int) this.engine.playbin.queryDuration(TimeUnit.SECONDS);
			
			// See if we need to notify owner of progress.
			if (positionSec != this.lastPositionSec) {
				this.engine.callPositionListener(positionSec);
				this.lastPositionSec = positionSec;
			}
			
			// See if we need to notify owner of duration change.
			if (durationSec != this.lastDurationSec) {
				this.engine.callDurationListener(durationSec);
				this.lastDurationSec = durationSec;
			}
			
			// End of track and GStreamer has failed to notify us?
			if (durationSec > 0 && positionSec >= (durationSec - Constants.EOS_MARGIN_SECONDS)) {
				this.eosManCounter = this.eosManCounter + 1;
				this.logger.fine("eosManCounter++ = " + this.eosManCounter);
				if (this.eosManCounter >= Constants.EOS_MAN_LIMIT) {
					this.eosManCounter = 0;
					this.engine.handleEosEvent("m=" + positionSec + "ns >= " + durationSec + "ns");
				}
			}
			
			// Poke screen saver.
			if (this.engine.hasVideo.get() && this.engine.m_playbackState == PlayState.Playing) {
				ScreenSaver.pokeScreenSaverProtected();
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
