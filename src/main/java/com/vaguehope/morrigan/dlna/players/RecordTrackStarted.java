package com.vaguehope.morrigan.dlna.players;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.util.ErrorHelper;

final class RecordTrackStarted implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(RecordTrackStarted.class);

	private final PlayItem item;

	public RecordTrackStarted (final PlayItem item) {
		this.item = item;
	}

	@Override
	public void run () {
		try {
			this.item.getList().incTrackStartCnt(this.item.getTrack());
		}
		catch (final Exception e) { // NOSONAR no other way to report errors.
			LOG.info("Failed to increment track start count: " + ErrorHelper.getCauseTrace(e));
		}
	}

}
