package com.vaguehope.morrigan.rpc.client;

import java.net.InetAddress;

import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.model.exceptions.MorriganException;

public class LocalHostServer {

	private static final Logger LOG = LoggerFactory.getLogger(LocalHostServer.class);

	private final InetAddress bindAddress;
	private final Server server;

	public LocalHostServer(HttpServlet servlet) {
		this.bindAddress = InetAddress.getLoopbackAddress();
		this.server = makeServer(this.bindAddress, servlet);
	}

	@SuppressWarnings("resource")
	private static Server makeServer(final InetAddress bindAddress, HttpServlet servlet) {
		final Server server = new Server();

		final ServerConnector connector = new ServerConnector(server);
		connector.setHost(bindAddress.getHostAddress());
		connector.setPort(0);
		server.addConnector(connector);

		final ServletContextHandler servletHandler = new ServletContextHandler();
		servletHandler.setContextPath("/");
		servletHandler.addServlet(new ServletHolder(servlet), "/");

		final HandlerList handler = new HandlerList();
		handler.setHandlers(new Handler[] { servletHandler });
		server.setHandler(handler);

		return server;
	}

	public void start() throws MorriganException {
		try {
			this.server.start();
			LOG.info("External URL: {}", getExternalHttpUrl());
		}
		catch (final Exception e) {
			throw new MorriganException(e);
		}
	}

	public void shutdown() throws Exception {
		this.server.stop();
	}

	public String uriFor (final String path) {
		return String.format("%s/%s", getExternalHttpUrl(), path);
	}

	public String getExternalHttpUrl () {
		return "http://" + this.bindAddress.getHostAddress() + ":" + ((ServerConnector) this.server.getConnectors()[0]).getLocalPort();
	}

}
