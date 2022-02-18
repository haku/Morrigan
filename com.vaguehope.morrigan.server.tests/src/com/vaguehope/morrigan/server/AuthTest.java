package com.vaguehope.morrigan.server;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vaguehope.morrigan.config.Config;

// Jetty source: https://github.com/eclipse/jetty.project/tree/jetty-7.5.4.v20111024/

public class AuthTest {

	private ScheduledExecutorService schEs;

	private Server server;
	private FixtureServlet fixtureServlet;

	@Before
	public void before () throws Exception {
		this.schEs = Executors.newScheduledThreadPool(1);
		startServer();
	}

	@Test
	public void itDoesSomething () throws Exception {
		this.fixtureServlet.addGetFixture("/f.ttf", "font/ttf", "desu");
		final URL url = new URL("http", "localhost", this.server.getConnectors()[0].getLocalPort(), "/f.ttf");
		System.out.println(url);
		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("Host", "localhost:" + url.getPort());
		conn.setRequestProperty("Authorization", "Basic YTpodW50ZXIy");
		conn.setRequestProperty("User-Agent", "curl/7.68.0");
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("Origin", "http://localhost:" + url.getPort());
		assertEquals(200, conn.getResponseCode());
	}

	private void startServer () throws Exception {
		this.server = new Server();

		final SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(0); // auto-bind to available port
		this.server.addConnector(connector);

		final ServletContextHandler contextHandler = new ServletContextHandler();
		contextHandler.setContextPath("/");
		this.fixtureServlet = new FixtureServlet();
		contextHandler.addServlet(new ServletHolder(this.fixtureServlet), "/*");
		this.server.setHandler(contextHandler);

		final AuthChecker authChecker = new AuthChecker() {
			@Override
			public boolean verifyAuth (final String passToTest) {
				return true;
			}
		};
		final AuthFilter authFilter = new AuthFilter(authChecker, Config.DEFAULT, this.schEs);
		final FilterHolder authFilterHolder = new FilterHolder(authFilter);
		contextHandler.addFilter(authFilterHolder, "/*", null);

		this.server.start();
	}

	@After
	public void stopServer () throws Exception {
		if (this.server != null) this.server.stop();
		if (this.schEs != null) this.schEs.shutdownNow();
	}

}
