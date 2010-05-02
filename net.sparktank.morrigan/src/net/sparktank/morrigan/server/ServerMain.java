package net.sparktank.morrigan.server;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.gui.helpers.ErrorHelper;
import net.sparktank.morrigan.model.MediaList;
import net.sparktank.morrigan.model.playlist.PlayItem;
import net.sparktank.morrigan.player.IPlayerEventHandler;
import net.sparktank.morrigan.player.Player;
import net.sparktank.morrigan.player.PlayerRegister;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;

import org.eclipse.swt.widgets.Composite;

public class ServerMain {

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public MorriganServer makeServer () throws Exception {
		return new MorriganServer();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void main(String[] args) throws Exception {
		// Prep player.
		setupPlayer();
		
		// Start server.
		MorriganServer s = makeServer();
		s.getServer().start();
		s.getServer().join(); // Like Thread.join().
		
		// Clean up.
		cleanupPlayer();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static Player _player;
	
	static private void setupPlayer () {
		_player = PlayerRegister.makePlayer(eventHandler);
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
			System.err.println("Throwable=" + ErrorHelper.getStackTrace(t));
		}
		
		@Override
		public boolean doOnForegroudThread(Runnable r) {
			return false;
		}
		@Override
		public Composite getCurrentMediaFrameParent() {
			return null;
		}
		@Override
		public MediaList getCurrentList() {
			return null;
		}
		@Override
		public void currentItemChanged() {}
		@Override
		public void historyChanged() {}
		@Override
		public void videoAreaSelected() {}
		@Override
		public void videoAreaClose() {}
	};
	
	
	static private PlayState prevPlayState = null;
	
	static private void outputStatus () {
		PlayState playState = _player.getPlayState();
		if (playState != prevPlayState) {
			prevPlayState = playState;
			
			PlayItem currentItem = _player.getCurrentItem();
			if (currentItem.item != null) {
				System.out.println(prevPlayState.toString() + " " + currentItem.item);
			} else {
				System.out.println(prevPlayState.toString());
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
