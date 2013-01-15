package com.vaguehope.morrigan.playbackimpl.gs;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public final class ScreenSaver {

	private ScreenSaver () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final Logger logger = Logger.getLogger(ScreenSaver.class.getName());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String FILE = "/usr/bin/gnome-screensaver-command";
	private static final String CMD = FILE + " --poke";

	public static boolean pokeScreenSaver () throws IOException {
		File file = new File(FILE); // FIXME search $PATH?
		if (file.exists()) {
			Process proc = null;
			try {
				proc = Runtime.getRuntime().exec(CMD);
				proc.waitFor();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			finally {
				if (proc != null) proc.destroy();
			}
			logger.fine("Screensaver poked.");
			return true;
		}

		logger.warning("File '" + file.getAbsolutePath() + "' not found, screen-saver will not be inhibited.");
		return false;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final long POKE_INTERVAL = 1000 * 60; // 60 seconds.

	private static final Lock lastPokeLock = new ReentrantLock();
	private static final AtomicLong lastPokeTime = new AtomicLong(0);

	public static void pokeScreenSaverProtected () {
		if (lastPokeTime.get() == 0 || System.currentTimeMillis() - lastPokeTime.get() > POKE_INTERVAL) {
			lastPokeLock.lock();
			try {
				if (lastPokeTime.get() == 0 || System.currentTimeMillis() - lastPokeTime.get() > POKE_INTERVAL) {
					lastPokeTime.set(System.currentTimeMillis());
					pokeScreenSaver();
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
			finally {
				lastPokeLock.unlock();
			}
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
