package com.vaguehope.morrigan.dlna.players;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.util.ErrorHelper;

final class RecordTrackCompleted implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(RecordTrackCompleted.class);

	private final PlayItem item;

	public RecordTrackCompleted (final PlayItem item) {
		this.item = item;
	}

	@Override
	public void run () {
		try {
			this.item.getList().incTrackEndCnt(this.item.getTrack());
		}
		catch (final Exception e) { // NOSONAR no other way to report errors.
			LOG.info("Failed to increment track end count: " + ErrorHelper.getCauseTrace(e));
		}
	}

}
