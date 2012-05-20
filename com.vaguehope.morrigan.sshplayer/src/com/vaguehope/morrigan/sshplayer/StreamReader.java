package com.vaguehope.morrigan.sshplayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StreamReader extends Thread {

	private static final Logger LOG = Logger.getLogger(StreamReader.class.getName());

	private final InputStream source;
	private final OutputStream target;
	private final AtomicBoolean finished = new AtomicBoolean(false);

	public StreamReader (InputStream source, OutputStream target) {
		setDaemon(true);
		this.source = source;
		this.target = target;
	}

	@Override
	public void run () {
		try {
			copy();
		}
		catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to complete copying stream.", e);
		}
		finally {
			this.finished.set(true);
		}
	}

	public boolean isFinished () {
		return this.finished.get();
	}

	private void copy () throws IOException {
		try {
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = this.source.read(buffer)) != -1) {
				if (this.target != null) this.target.write(buffer, 0, bytesRead);
			}
			if (this.target != null) this.target.flush();
		}
		finally {
			this.source.close();
		}
	}

}
