package com.vaguehope.morrigan.player;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.util.ErrorHelper;

public class PlaybackRecorder {

	private static final Logger LOG = LoggerFactory.getLogger(PlaybackRecorder.class);

	private final ScheduledExecutorService schEx;

	public PlaybackRecorder(final ScheduledExecutorService schEx) {
		this.schEx = schEx;
	}

	public void recordStarted(final PlayItem playItem) {
		this.schEx.schedule(() -> {
			try {
				playItem.getList().incTrackStartCnt(playItem.getItem());
			}
			catch (MorriganException e) {
				LOG.info("Failed to record track started: " + ErrorHelper.getCauseTrace(e));
			}
		}, 5, TimeUnit.SECONDS);
	}

	public void recordCompleted(final PlayItem playItem) {
		this.schEx.schedule(() -> {
			try {
				playItem.getList().incTrackEndCnt(playItem.getItem());
			}
			catch (MorriganException e) {
				LOG.info("Failed to record track completed: " + ErrorHelper.getCauseTrace(e));
			}
		}, 5, TimeUnit.SECONDS);
	}

}
