package com.vaguehope.morrigan.dlna.players;

import com.vaguehope.morrigan.player.PlayItem;

final class OnTrackComplete implements Runnable {

	private final AbstractDlnaPlayer dlnaPlayer;
	private final PlayItem item;

	public OnTrackComplete (final AbstractDlnaPlayer dlnaPlayer, final PlayItem item) {
		this.dlnaPlayer = dlnaPlayer;
		this.item = item;
	}

	@Override
	public void run () {
		this.dlnaPlayer.recordTrackCompleted(this.item);
		this.dlnaPlayer.nextTrack();
	}

}
