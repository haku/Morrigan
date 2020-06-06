package com.vaguehope.morrigan.sshplayer;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.vaguehope.morrigan.server.ServletHelper;

public class FileServer {

	private final File file;
	private final int port;

	private Server server;

	public FileServer (File file, int port) {
		this.file = file;
		this.port = port;
	}

	public void start () {
		this.server = new Server(this.port);
		AbstractHandler handler = new FileSenderHandler(this.file);
		this.server.setHandler(handler);
		try {
			this.server.start();
		}
		catch (Exception e) {
			throw new IllegalStateException(e); // FIXME wrap in something better,
		}
	}

	public void stop () {
		try {
			this.server.stop();
		}
		catch (Exception e) {
			throw new IllegalStateException(e); // FIXME wrap in something better,
		}
	}

	private static class FileSenderHandler extends AbstractHandler {

		private final File file;

		public FileSenderHandler (File file) {
			this.file = file;
		}

		@Override
		public void handle (String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			try {
				ServletHelper.returnFile(this.file, null, null, null, response);
			}
			catch (IOException e) {
				// Do not care; its the client's problem.
			}
			catch (Exception e) {
				if (e instanceof InterruptedException) return;
				CliPlayer.LOG.log(Level.WARNING, "Failed to serve media file.", e);
			}
		}

	}

}
