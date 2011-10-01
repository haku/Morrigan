package com.vaguehope.morrigan.playbackimpl.vlc;

import uk.co.caprica.vlcj.player.MediaPlayer;

public class VlcHelper {
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private VlcHelper () { /* Unused */}
	
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public int readFileDuration (final String fpath) {
		MediaPlayer player = Activator.getFactory().newMediaPlayer();
		player.mute();
		player.prepareMedia(fpath);
		long queryLength = -1;
		long startTime = System.currentTimeMillis();
		while (true) {
			queryLength = player.getLength();
			if (queryLength > 0 || System.currentTimeMillis() - startTime > Constants.FILE_READ_DURATION_TIMEOUT) {
				break;
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) { /* UNUSED */ }
		}
		player.release();
		
		int retDuration = -1;
		if (queryLength > 0) {
			retDuration = (int) (queryLength / 1000);
			if (retDuration < 1) retDuration = 1;
		}
		
		return retDuration;
		
		// TODO use this after upgrade.
		// https://code.google.com/p/vlcj/source/browse/trunk/vlcj/src/test/java/uk/co/caprica/vlcj/test/meta/MetaTest.java
//		MediaMeta mediaMeta = mediaPlayer.getMediaMeta(fpath, true);
	}
	
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
