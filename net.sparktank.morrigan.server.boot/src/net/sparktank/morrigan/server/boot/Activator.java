package net.sparktank.morrigan.server.boot;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMediaTrackList;
import net.sparktank.morrigan.player.IPlayerEventHandler;
import net.sparktank.morrigan.player.IPlayerLocal;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;
import net.sparktank.morrigan.player.PlayItem;
import net.sparktank.morrigan.player.PlayerRegister;
import net.sparktank.morrigan.server.MorriganServer;

import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static protected final Logger logger = Logger.getLogger(Activator.class.getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MorriganServer server;
	
	@Override
	public void start (BundleContext context) throws Exception {
		// Prep player.
		setupPlayer();
		
		// Start server.
		this.server = makeServer();
		this.server.start();
		logger.fine("Morrigan Server listening on port [TODO insert port number here].");
	}
	
	@Override
	public void stop (BundleContext context) throws Exception {
		this.server.stop();
		logger.fine("Morrigan Server stopped.");
		
		// Clean up.
		cleanupPlayer();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public MorriganServer makeServer () throws Exception {
		return new MorriganServer();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static IPlayerLocal _player;
	
	static private void setupPlayer () {
		_player = PlayerRegister.makeLocalPlayer(eventHandler);
		_player.setPlaybackOrder(PlaybackOrder.RANDOM);
	}
	
	static private void cleanupPlayer () {
		_player.dispose();
	}
	
	static private IPlayerEventHandler eventHandler = new IPlayerEventHandler() {
		
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
	
	
	static private PlayState prevPlayState = null;
	
	static void outputStatus () {
		PlayState playState = _player.getPlayState();
		if (playState != prevPlayState) {
			prevPlayState = playState;
			
			PlayItem currentItem = _player.getCurrentItem();
			if (currentItem.item != null) {
				System.out.println(playState.toString() + " " + currentItem.item);
			} else {
				System.out.println(playState.toString());
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
