package com.vaguehope.morrigan.server.boot;

import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.screen.ScreenMgrCallback;

class ServerScreenMgrCallback implements ScreenMgrCallback {

	private final ServerPlayerEventHandler eventHandler;

	public ServerScreenMgrCallback (ServerPlayerEventHandler eventHandler) {
		if (eventHandler == null) throw new IllegalArgumentException();
		this.eventHandler = eventHandler;
	}

	@Override
	public void updateCurrentMediaFrameParent (Composite parent) {
		LocalPlayer player = this.eventHandler.getPlayer();
		if (player != null && player.isPlaybackEngineReady()) {
			player.setVideoFrameParent(parent);
		}
	}

	@Override
	public void handleError (Exception e) {
		this.eventHandler.asyncThrowable(e);
	}

}