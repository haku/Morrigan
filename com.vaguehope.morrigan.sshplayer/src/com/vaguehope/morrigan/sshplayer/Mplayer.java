package com.vaguehope.morrigan.sshplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * References:
 * http://www.jcraft.com/jsch/examples/
 * http://www.mplayerhq.hu/DOCS/tech/slave.txt
 *
 */
public class Mplayer extends Thread {

	protected static final Logger LOG = Logger.getLogger(Mplayer.class.getName());

	private static final String MPLAYER_CMD =
			"export DISPLAY=:0 ;" +
					" [ -e ~/.mnmpcmd ] || mkfifo ~/.mnmpcmd ;" +
					" mplayer -input file=~/.mnmpcmd -cache 8192 -identify -fs - ;" +
					" killall mplayer > /dev/null 2>&1 ;" +
					" echo " + MplayerStatusReader.MORRIGAN_EOF;

	private enum Commands {
		TOGGLE_PAUSED
	}

	private static final int CONNECT_TIMEOUT = 15000;

	private final MplayerHost mplayerHost;
	private final File media;

	protected final AtomicBoolean running = new AtomicBoolean(false);
	protected final AtomicBoolean canceled = new AtomicBoolean(false);
	private final Queue<Commands> cmd = new ConcurrentLinkedQueue<Commands>();
	private final AtomicReference<MplayerStatusReader> status = new AtomicReference<MplayerStatusReader>();

	public Mplayer (MplayerHost mplayerHost, File media) {
		this.mplayerHost = mplayerHost;
		this.media = media;
	}

	@Override
	public void run () {
		if (!this.running.compareAndSet(false, true)) throw new IllegalStateException("Already started.");
		try {
			runPlayback();
		}
		catch (Exception e) {
			// TODO log failure to some form of player status.
			LOG.log(Level.WARNING, "Playback failed.", e);
		}
		finally {
			Mplayer.this.running.set(false);
			LOG.info("Mplayer completed: " + this.media.getAbsolutePath());
		}
	}

	public void cancel () throws InterruptedException {
		if (this.canceled.compareAndSet(false, true)) {
			LOG.info("Cancelling mplayer: " + this.media.getAbsolutePath());
			if (this.isAlive() && Thread.currentThread().getId() != this.getId()) this.join();
			LOG.info("Mplayer canceled: " + this.media.getAbsolutePath());
		}
		LOG.warning("Mplayer already canceled.");
	}

	public boolean isRunning () {
		return this.running.get();
	}

	public void togglePaused () {
		if (isRunning()) this.cmd.add(Commands.TOGGLE_PAUSED);
	}

	public int getCurrentPosition () {
		MplayerStatusReader s = this.status.get();
		return s == null ? -1 : s.getCurrentPosition();
	}

	public int getDuration () {
		MplayerStatusReader s = this.status.get();
		return s == null ? -1 : s.getDuration();
	}

	protected void runPlayback () throws JSchException, IOException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(this.mplayerHost.getUser(), this.mplayerHost.getHost(), this.mplayerHost.getPort());
		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.setUserInfo(this.mplayerHost.getUserinfo());
		session.connect();
		try {
			ChannelExec execCh = (ChannelExec) session.openChannel("exec");
			execCh.setCommand(MPLAYER_CMD);
			execCh.setInputStream(new FileInputStream(this.media)); // Let JSch copy the stream.
			execCh.connect(CONNECT_TIMEOUT);

			MplayerStatusReader statusReader = new MplayerStatusReader(execCh.getInputStream());
			statusReader.start();
			this.status.set(statusReader);

			new StreamReader(execCh.getErrStream(), System.err).start();

			while (!this.canceled.get()) {
				if (statusReader.isFinished() || execCh.isEOF() || execCh.isClosed() || !execCh.isConnected()) {
					break;
				}
				procCmd(session);
				try {
					Thread.sleep(1000);
				}
				catch (Exception ee) {/* Ignore. */}
			}

			try {
				execCh.sendSignal("KILL");
			}
			catch (Exception e) {
				LOG.log(Level.WARNING, "Exception trying to KILL remote process playing: " + this.media.getAbsolutePath() , e);
			}
			execCh.disconnect();
		}
		finally {
			session.disconnect();
		}
	}

	public void procCmd (Session session) throws JSchException {
		Commands c = this.cmd.poll();
		if (c == null) return;
		if (c == Commands.TOGGLE_PAUSED) {
			// Must be a nicer way to do this.
			ChannelExec cmdExCh = (ChannelExec) session.openChannel("exec");
			cmdExCh.setCommand("echo pause > ~/.mnmpcmd");
			cmdExCh.connect();
			cmdExCh.disconnect();
		}
		else {
			LOG.warning("Unknown command: " + c);
		}
	}

}
