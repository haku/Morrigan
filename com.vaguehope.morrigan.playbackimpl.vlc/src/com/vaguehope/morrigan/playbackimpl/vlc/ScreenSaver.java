package com.vaguehope.morrigan.playbackimpl.vlc;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public final class ScreenSaver {

	private static final long POKE_INTERVAL_MILLIS = 60L * 1000L;
	private static final String FILE = "/usr/bin/gnome-screensaver-command";
	private static final String CMD = FILE + " --poke";
	private static final Logger LOG = Logger.getLogger(ScreenSaver.class.getName());

	private static final AtomicBoolean canPokeScreensaver = new AtomicBoolean(true);
	private static final Lock lastPokeLock = new ReentrantLock();
	private static final AtomicLong lastPokeTime = new AtomicLong(0);

	private ScreenSaver () {}

	public static void poke () {
		if (!canPokeScreensaver.get()) return;
		if (isPokeDue()) {
			lastPokeLock.lock();
			try {
				if (isPokeDue()) {
					lastPokeTime.set(System.currentTimeMillis());
					if (!pokeCmd()) canPokeScreensaver.set(false);
				}
			}
			catch (final IOException e) {
				throw new IllegalStateException(e);
			}
			finally {
				lastPokeLock.unlock();
			}
		}
	}

	private static boolean isPokeDue () {
		return lastPokeTime.get() == 0 || System.currentTimeMillis() - lastPokeTime.get() > POKE_INTERVAL_MILLIS;
	}

	private static boolean pokeCmd () throws IOException {
		final File file = new File(FILE); // FIXME search $PATH?
		if (file.exists()) {
			Process proc = null;
			try {
				proc = Runtime.getRuntime().exec(CMD);
				proc.waitFor();
			}
			catch (final InterruptedException e) {
				throw new IllegalStateException(e);
			}
			finally {
				if (proc != null) proc.destroy();
			}
			LOG.fine("Screensaver poked.");
			return true;
		}

		LOG.warning("File '" + file.getAbsolutePath() + "' not found, screen-saver will not be inhibited.");
		return false;
	}

}
