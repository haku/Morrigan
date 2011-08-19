package com.vaguehope.morrigan.server;

import java.util.Map;


import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.IPlayerEventHandler;
import com.vaguehope.morrigan.player.IPlayerLocal;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.util.ErrorHelper;

@Deprecated
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
		s.start();
		s.join(); // Like Thread.join().
		
		// Clean up.
		cleanupPlayer();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static IPlayerLocal _player;
	
	static private void setupPlayer () {
		_player = PlayerRegister.makeLocalPlayer("Server", eventHandler); // TODO why is this called in 2 places???
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
	
	
	static private PlayState prevPlayState = null;
	
	static void outputStatus () {
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
