package com.vaguehope.morrigan.sshplayer;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
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
 * References: http://www.jcraft.com/jsch/examples/
 */
public class CliPlayer extends Thread {

	protected static final Logger LOG = Logger.getLogger(CliPlayer.class.getName());

	private static final int CONNECT_TIMEOUT = 15000; // 15 seconds.

	private final CliHost host;
	private final File media;

	protected final AtomicBoolean running = new AtomicBoolean(false);
	protected final AtomicBoolean canceled = new AtomicBoolean(false);
	private final Queue<CliPlayerCommand> cmd = new ConcurrentLinkedQueue<CliPlayerCommand>();
	private final AtomicReference<CliStatusReader> status = new AtomicReference<CliStatusReader>();

	public CliPlayer (CliHost host, File media) {
		this.host = host;
		this.media = media;
	}

	@Override
	public void run () {
		if (!this.running.compareAndSet(false, true)) throw new IllegalStateException("Already started.");
		try {
			runPlaybackSession();
		}
		catch (Exception e) {
			// TODO log failure to some form of player status.
			LOG.log(Level.WARNING, "Playback failed.", e);
		}
		finally {
			CliPlayer.this.running.set(false);
			LOG.info("CliPlayer completed: " + this.media.getAbsolutePath());
		}
	}

	public void cancel () throws InterruptedException {
		if (this.canceled.compareAndSet(false, true)) {
			LOG.info("Cancelling CliPlayer: " + this.media.getAbsolutePath());
			if (this.isAlive() && Thread.currentThread().getId() != this.getId()) this.join();
		}
		LOG.warning("CliPlayer already cancelled.");
	}

	public boolean isRunning () {
		return this.running.get();
	}

	public void togglePaused () {
		if (isRunning()) this.cmd.add(this.host.cmds().pauseResumeCommand());
	}

	public int getCurrentPosition () {
		CliStatusReader s = this.status.get();
		return s == null ? -1 : s.getCurrentPosition();
	}

	public int getDuration () {
		CliStatusReader s = this.status.get();
		return s == null ? -1 : s.getDuration();
	}

	protected void runPlaybackSession () throws JSchException, IOException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(this.host.getUser(), this.host.getHost(), this.host.getPort());
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.setUserInfo(this.host.getUserinfo());
		session.connect();
		try {
			runStream(session);
		}
		finally {
			session.disconnect();
		}
	}

	private void runStream (Session session) throws JSchException, IOException {
		session.setPortForwardingR(CliPlayerCommands.SHARED_PORT, "127.0.0.1", CliPlayerCommands.SHARED_PORT);
		FileServer fs = new FileServer(this.media, 34400);
		fs.start();
		try {
			runCliPlayer(session);
		}
		finally {
			fs.stop();
		}
	}

	private void runCliPlayer (Session session) throws JSchException, IOException {
		ChannelExec execCh = (ChannelExec) session.openChannel("exec");
		execCh.setCommand(this.host.cmds().startCommand(this.media));
		execCh.connect(CONNECT_TIMEOUT);

		CliStatusReader statusReader = this.host.cmds().makeStatusReader(execCh.getInputStream());
		statusReader.start();
		this.status.set(statusReader);

		new StreamReader(execCh.getErrStream(), System.err).start();

		while (!this.canceled.get()) {
			if (statusReader.isFinished() || execCh.isEOF() || execCh.isClosed() || !execCh.isConnected()) {
				break;
			}
			procCmd(session, execCh);
			try {
				Thread.sleep(1000L);
			}
			catch (Exception e) {/* Ignore. */}
		}

		CliPlayerHelper.execCommand(session, this.host.cmds().killCommand());
		execCh.disconnect();
	}

	private void procCmd (Session session, ChannelExec mainChEx) throws JSchException, IOException {
		CliPlayerCommand c = this.cmd.poll();
		if (c == null) return;
		c.exec(session, mainChEx);
	}

}
