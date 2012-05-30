package com.vaguehope.morrigan.sshplayer.mplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.sshplayer.CliStatusReader;

public class MplayerStatusReader extends Thread implements CliStatusReader {

	private static final String IDENT_LENGTH = "ID_LENGTH=";
	private static final String IDENT_EXIT = "ID_EXIT=";

	private static final String FIELD_VIDPOS = "V:";

	private static final Logger LOG = Logger.getLogger(MplayerStatusReader.class.getName());

	private final BufferedReader source;
	private final AtomicBoolean finished = new AtomicBoolean(false);
	private final AtomicInteger duration = new AtomicInteger(-1);
	private final AtomicInteger currentPosition = new AtomicInteger(-1);

	public MplayerStatusReader (InputStream source) {
		this.source = new BufferedReader(new InputStreamReader(source));
		setDaemon(true);
	}

	@Override
	public void run () {
		try {
			read();
		}
		catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to complete copying stream.", e);
		}
		finally {
			try {
				this.source.close();
			}
			catch (IOException e) {
				LOG.log(Level.WARNING, "Failed to close stream.", e);
			}
			finally {
				this.finished.set(true);
			}
		}
	}

	private void read () throws IOException {
		String line;
		while ((line = this.source.readLine()) != null) {
			if (!procLine(line)) break;
		}
	}

	@Override
	public boolean isFinished () {
		return this.finished.get();
	}

	@Override
	public int getDuration () {
		return this.duration.get();
	}

	@Override
	public int getCurrentPosition () {
		return this.currentPosition.get();
	}

	/**
	 * return:
	 *  true to keep going.
	 *  false to abort.
	 */
	private boolean procLine (String s) {
		if (s.length() < 1) return true;
		if (s.startsWith(IDENT_LENGTH)) {
			readLength(s);
		}
		else if (s.startsWith(IDENT_EXIT)) {
			return false;
		}
		else if (s.startsWith(MORRIGAN_EOF)) {
			return false;
		}
		else {
			readStatusLine(s);
		}
		return true;
	}

	private void readLength (String s) {
		this.duration.set(Integer.parseInt(s.substring(IDENT_LENGTH.length(), findNextNonDigit(s, IDENT_LENGTH.length() + 1))));
	}

	public void readStatusLine (String s) {
		int vidFieldPos = s.indexOf(FIELD_VIDPOS);
		if (vidFieldPos > 0) {
			int vidDataPos = findNextDigit(s, vidFieldPos + FIELD_VIDPOS.length());
			if (vidDataPos > vidFieldPos) {
				int vidDataEdPos = findNextSpace(s, vidDataPos);
				if (vidDataEdPos > vidDataPos) {
					String posStr = s.substring(vidDataPos, vidDataEdPos);
					this.currentPosition.set(Math.round(Float.parseFloat(posStr)));
				}
				else {
					this.currentPosition.set(-1);
				}
			}
			else {
				this.currentPosition.set(-1);
			}
		}
	}

	private static int findNextDigit (String s, int start) {
		for (int i = start; i < s.length(); i++) {
			if (Character.isDigit(s.charAt(i))) return i;
		}
		return s.length();
	}

	private static int findNextNonDigit (String s, int start) {
		for (int i = start; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) return i;
		}
		return s.length();
	}

	private static int findNextSpace (String s, int start) {
		for (int i = start; i < s.length(); i++) {
			if (Character.isSpaceChar(s.charAt(i))) return i;
		}
		return s.length();
	}

}
