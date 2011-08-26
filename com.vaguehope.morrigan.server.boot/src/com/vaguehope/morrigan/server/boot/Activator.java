package com.vaguehope.morrigan.server.boot;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.IPlayerEventHandler;
import com.vaguehope.morrigan.player.IPlayerLocal;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.server.MorriganServer;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static protected final Logger logger = Logger.getLogger(Activator.class.getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MorriganServer server;
	private IPlayerLocal player;
	
	@Override
	public void start (BundleContext context) throws Exception {
		// Start server.
		this.server = new MorriganServer();
		this.server.start();
		
		// Prep player.
		this.player = PlayerRegister.makeLocalPlayer("Server", this.eventHandler); // TODO why is this called in 2 places???
		this.player.setPlaybackOrder(PlaybackOrder.RANDOM);
	}
	
	@Override
	public void stop (BundleContext context) throws Exception {
		this.server.stop();
		logger.fine("Morrigan Server stopped.");
		
		// Clean up.
		this.player.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IPlayerEventHandler eventHandler = new IPlayerEventHandler() {
		
		@Override
		public void updateStatus() {
			outputStatus();
		}
		
		@Override
		public void asyncThrowable(Throwable t) {
			logger.log(Level.WARNING, "asyncThrowable", t);
		}
		
		@Override
		public Composite getCurrentMediaFrameParent() {
			return null;
		}
		@Override
		public Map<Integer, String> getMonitors() {
			return null;
		}
		@Override
		public void goFullscreen(int monitor) {/* UNUSED */}
		@Override
		public IMediaTrackList<IMediaTrack> getCurrentList() {
			return null;
		}
		@Override
		public void currentItemChanged() {/* UNUSED */}
		@Override
		public void historyChanged() {/* UNUSED */}
		@Override
		public void videoAreaSelected() {/* UNUSED */}
		@Override
		public void videoAreaClose() {/* UNUSED */}
	};
	
	
	private PlayState prevPlayState = null;
	
	protected void outputStatus () {
		PlayState playState = this.player.getPlayState();
		if (playState != this.prevPlayState) {
			this.prevPlayState = playState;
			
			PlayItem currentItem = this.player.getCurrentItem();
			if (currentItem.item != null) {
				System.out.println(playState.toString() + " " + currentItem.item);
			} else {
				System.out.println(playState.toString());
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
