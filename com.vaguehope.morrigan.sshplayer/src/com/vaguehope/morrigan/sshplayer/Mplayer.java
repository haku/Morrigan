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
 * http://www.mplayerhq.hu/DOCS/tech/slave.txt
 */
public class Mplayer extends Thread {

	protected static final Logger LOG = Logger.getLogger(Mplayer.class.getName());

	private enum Commands {
		TOGGLE_PAUSED
	}

	private static final int CONNECT_TIMEOUT = 15000; // 15 seconds.

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
			runPlaybackSession();
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
		}
		LOG.warning("Mplayer already cancelled.");
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

	protected void runPlaybackSession () throws JSchException, IOException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(this.mplayerHost.getUser(), this.mplayerHost.getHost(), this.mplayerHost.getPort());
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.setUserInfo(this.mplayerHost.getUserinfo());
		session.connect();
		try {
			runStream(session);
		}
		finally {
			session.disconnect();
		}
	}

	private void runStream (Session session) throws JSchException, IOException {
		session.setPortForwardingR(34400, "127.0.0.1", 34400); // TODO auto find free port.
		FileServer fs = new FileServer(this.media, 34400);
		fs.start();
		try {
			runMplayer(session);
		}
		finally {
			fs.stop();
		}
	}

	private void runMplayer (Session session) throws JSchException, IOException {
		ChannelExec execCh = (ChannelExec) session.openChannel("exec");
		execCh.setCommand(makeMplayerCommand());
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
			catch (Exception e) {/* Ignore. */}
		}

		exec(session, "killall mplayer > /dev/null 2>&1"); // Very blunt.
		execCh.disconnect();
	}

	private void procCmd (Session session) throws JSchException {
		Commands c = this.cmd.poll();
		if (c == null) return;
		if (c == Commands.TOGGLE_PAUSED) {
			exec(session, "echo pause > ~/.mnmpcmd"); // Must be a nicer way to do this.
		}
		else {
			LOG.warning("Unknown command: " + c);
		}
	}

	private String makeMplayerCommand () {
		StringBuilder s = new StringBuilder("cd"); // Start in home directory.
		s.append(" ; export DISPLAY=:0");
		s.append(" ; if [[ ! -e .mnmpcmd ]] ; then mkfifo .mnmpcmd ; fi");
		s.append(" ; mplayer -input file=.mnmpcmd -cache 131072 -identify -fs 'http://localhost:34400/")
				.append(genericFileName(this.media)).append("'");
		s.append(" ; echo ").append(MplayerStatusReader.MORRIGAN_EOF);
		return s.toString();
	}

	private static void exec (Session session, String command) throws JSchException {
		ChannelExec cmdExCh = (ChannelExec) session.openChannel("exec");
		cmdExCh.setCommand(command);
		cmdExCh.connect();
		cmdExCh.disconnect();
	}

	private static String genericFileName (File file) {
		String n = file.getName();
		return "file" + n.substring(n.lastIndexOf("."));
	}

}
