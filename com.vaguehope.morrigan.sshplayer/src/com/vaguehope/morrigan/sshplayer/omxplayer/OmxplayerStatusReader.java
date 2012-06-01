package com.vaguehope.morrigan.sshplayer.omxplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.sshplayer.CliStatusReader;
import com.vaguehope.morrigan.sshplayer.ParserHelper;

public class OmxplayerStatusReader extends Thread implements CliStatusReader {

	private static final String FIELD_VIDPOS = "V :";

	private static final Logger LOG = Logger.getLogger(OmxplayerStatusReader.class.getName());
	
	private final BufferedReader source;
	private final AtomicBoolean finished = new AtomicBoolean(false);
	private final AtomicInteger duration = new AtomicInteger(-1);
	private final AtomicInteger currentPosition = new AtomicInteger(-1);

	public OmxplayerStatusReader (InputStream source) {
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
		if (s.startsWith(MORRIGAN_EOF)) return false;
		readStatusLine(s);
		return true;
	}

	public void readStatusLine (String s) {
		int vidFieldPos = s.indexOf(FIELD_VIDPOS);
		if (vidFieldPos >= 0) {
			int vidDataPos = ParserHelper.findNextDigit(s, vidFieldPos + FIELD_VIDPOS.length());
			if (vidDataPos > vidFieldPos) {
				int vidDataEdPos = ParserHelper.findNextSpace(s, vidDataPos);
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

}
