package com.vaguehope.morrigan.playbackimpl.vlc;

import java.io.File;
import java.io.FileNotFoundException;

import uk.co.caprica.vlcj.player.MediaPlayer;

public class VlcHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private VlcHelper () { /* Unused */}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/* Would like to be able to do something like this, but it does not include
	 * length.
	 * https://code.google.com/p/vlcj/source/browse/trunk/vlcj/src/test/java/uk/co/caprica/vlcj/test/meta/MetaTest.java
	 */
	public static int readFileDuration (final String fpath) {
		int ret = -1;
		MediaPlayer player = Activator.getFactory().newHeadlessMediaPlayer();
		try {
			player.prepareMedia(fpath, "novideo"); // mediaOptions seem to be CLI options without the "--".
			player.mute();
			player.play();
			player.pause();
			long startTime = System.currentTimeMillis();
			long length;
			while (true) {
				length = player.getLength();
				if (length > 0 || System.currentTimeMillis() - startTime > Constants.FILE_READ_DURATION_TIMEOUT) {
					break;
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) { /* UNUSED */ }
			}

			if (length > 0) {
				ret = (int) (length / 1000);
				if (ret < 1) ret = 1;
			}
		}
		finally {
			player.release();
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final static String TEST_FILE = "/test/test.avi";

	public static void main (String[] args) throws FileNotFoundException {
		File file = new File(System.getProperty("user.home") + TEST_FILE);
		if (!file.exists()) throw new FileNotFoundException(TEST_FILE);

		Activator.createFactory();
		int duration = readFileDuration(file.getAbsolutePath());
		System.out.println("duration=" + duration);
		Activator.destroyFactory();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
