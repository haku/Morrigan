package com.vaguehope.morrigan.sshui;

import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;

import com.vaguehope.morrigan.util.DaemonThreadFactory;

public class MnCommandFactory implements ShellFactory {

	private static final int MAX_CLIENTS = 10;

	private final MnContext mnContext;
	private final ThreadPoolExecutor es;

	public MnCommandFactory (final MnContext mnContext) {
		this.mnContext = mnContext;
		this.es = new ThreadPoolExecutor(0, MAX_CLIENTS,
				1L, TimeUnit.MINUTES,
				new SynchronousQueue<Runnable>(),
				new DaemonThreadFactory("sshui"));
		this.es.allowCoreThreadTimeOut(true);
	}

	public void shutdown () {
		this.es.shutdownNow();
	}

	@Override
	public Command createShell(ChannelSession channel) throws IOException {
		return new MnCommand(this.mnContext, this.es);
	}

}
