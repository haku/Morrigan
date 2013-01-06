package com.vaguehope.morrigan.server.boot;

import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.screen.ScreenMgrCallback;

class ServerScreenMgrCallback implements ScreenMgrCallback {

	private final ServerPlayerEventHandler eventHandler;
	private final NullScreen nullScreen;

	public ServerScreenMgrCallback (ServerPlayerEventHandler eventHandler, NullScreen nullScreen) {
		if (eventHandler == null) throw new IllegalArgumentException();
		if (nullScreen == null) throw new IllegalArgumentException();
		this.eventHandler = eventHandler;
		this.nullScreen = nullScreen;
	}

	@Override
	public Composite getCurrentScreen () {
		return this.nullScreen.getScreen();
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
