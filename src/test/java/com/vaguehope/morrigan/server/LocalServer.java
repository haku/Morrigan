package com.vaguehope.morrigan.server;

import java.net.InetAddress;
import java.net.URI;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class LocalServer {

	private final Server server;
	private final ServletContextHandler contextHandler;
	private final FixtureServlet fixtureServlet;
	private final URI uri;

	public LocalServer() throws Exception {
		this.server = new Server();

		@SuppressWarnings("resource")
		final ServerConnector connector = new ServerConnector(this.server);
		final String hostAddress = InetAddress.getLocalHost().getHostAddress();
		connector.setHost(hostAddress);
		connector.setPort(0);
		this.server.addConnector(connector);

		this.contextHandler = new ServletContextHandler();
		this.contextHandler.setContextPath("/");
		this.server.setHandler(this.contextHandler);

		this.fixtureServlet = new FixtureServlet();
		this.contextHandler.addServlet(new ServletHolder(this.fixtureServlet), "/*");

		this.server.start();
		this.uri = new URI("http", "user:passwd", hostAddress, connector.getLocalPort(), "/", null, null);
	}

	public ServletContextHandler getContextHandler() {
		return this.contextHandler;
	}

	public FixtureServlet getFixtureServlet() {
		return this.fixtureServlet;
	}

	public URI getUri() {
		return this.uri;
	}

	public void dispose() {

	}

}
