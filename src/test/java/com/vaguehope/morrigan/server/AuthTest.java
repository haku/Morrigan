package com.vaguehope.morrigan.server;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jetty.servlet.FilterHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.config.Config;

// Jetty source: https://github.com/eclipse/jetty.project/tree/jetty-7.5.4.v20111024/

public class AuthTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private Config config;
	private LocalServer server;
	private ScheduledExecutorService schEs;

	@Before
	public void before () throws Exception {
		this.config = new Config(this.tmp.getRoot());
		this.server = new LocalServer();
		this.schEs = Executors.newScheduledThreadPool(1);
		setupMockServer();
	}

	@After
	public void stopServer () throws Exception {
		if (this.server != null) this.server.dispose();
		if (this.schEs != null) this.schEs.shutdownNow();
	}

	@Test
	public void itDoesSomething () throws Exception {
		this.server.getFixtureServlet().addGetFixture("/f.ttf", "font/ttf", "desu");
		final URL url = this.server.getUri().resolve("/f.ttf").toURL();
		System.out.println(url);
		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("Host", "localhost:" + url.getPort());
		conn.setRequestProperty("Authorization", "Basic YTpodW50ZXIy");
		conn.setRequestProperty("User-Agent", "curl/7.68.0");
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("Origin", "http://localhost:" + url.getPort());
		assertEquals(200, conn.getResponseCode());
	}

	public void setupMockServer() throws Exception {
		final AuthChecker authChecker = new AuthChecker() {
			@Override
			public boolean verifyAuth (final String user, final String pass) {
				return true;
			}
		};
		final AuthFilter authFilter = new AuthFilter(authChecker, this.config, this.schEs);
		final FilterHolder authFilterHolder = new FilterHolder(authFilter);
		this.server.getContextHandler().addFilter(authFilterHolder, "/*", null);
	}

}
