package com.vaguehope.morrigan.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.webapp.WebAppContext;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.wui.MorriganWui;

public class MorriganServer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private final Server server;
	private final int serverPort;

	private Runnable onStopRunnable = null;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MorriganServer (BundleContext context, ServerConfig config, PlayerReader playerListener, MediaFactory mediaFactory, AsyncTasksRegister asyncTasksRegister, AsyncActions asyncActions) throws MorriganException {
		try {
			// Config.
			this.serverPort = config.getServerPort();

			// Jetty server instance.
			org.eclipse.jetty.util.log.Log.setLog(new JettyLogger()); // Fix noisy logging.
			this.server = new Server();
			this.server.addLifeCycleListener(this.lifeCycleListener);

			// HTTP connector.
			Connector connector = new SocketConnector();
			connector.setPort(this.getServerPort());
			this.server.addConnector(connector);

			// HTTPS.
//			final SslContextFactory sslContextFactory = new SslContextFactory(sKeyStore);
//			sslContextFactory.setKeyStorePassword(sPassword);
//			final SslSocketConnector sslConnector = new SslSocketConnector(sslContextFactory);
//			sslConnector.setReuseAddress(true);
//			sslConnector.setPort(8443);
//			this.server.addConnector(sslConnector);

			// Auth filter to control access.
			Filter authFilter = new AuthFilter(config);
			FilterHolder filterHolder = new FilterHolder(authFilter);

			// This will hold all our servlets and webapps.
			ContextHandlerCollection contexts = new ContextHandlerCollection();
			this.server.setHandler(contexts);

			// Servlets.
			ServletContextHandler servletContext = new ServletContextHandler(contexts, "/", ServletContextHandler.SESSIONS);
			servletContext.addFilter(filterHolder, "/*", null);
			servletContext.addServlet(new ServletHolder(new PlayersServlet(playerListener)), PlayersServlet.CONTEXTPATH + "/*");
			servletContext.addServlet(new ServletHolder(new MlistsServlet(playerListener, mediaFactory, asyncActions)), MlistsServlet.CONTEXTPATH + "/*");
			servletContext.addServlet(new ServletHolder(new StatusServlet(asyncTasksRegister)), StatusServlet.CONTEXTPATH + "/*");

			// Web UI in WAR file.
			WebAppContext warContext = WebAppHelper.getWarBundleAsContext(context, MorriganWui.ID, "/");
			warContext.addFilter(filterHolder, "/*", null);
			contexts.addHandler(warContext);
		}
		catch (Exception e) {
			throw new MorriganException("Failed to configure and start server.", e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void start () throws Exception {
		this.server.start();
	}

	public void stop () throws Exception {
		this.server.stop();
	}

	public void join () throws InterruptedException {
		this.server.join();
	}

	public int getServerPort () {
		return this.serverPort;
	}

	public void setOnStopRunnable (Runnable r) {
		this.onStopRunnable = r;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Jetty Server listener.

	private Listener lifeCycleListener = new Listener() {

		@Override
		public void lifeCycleStarting (LifeCycle lc) {
			getLogger().fine("Server starting...");
		}

		@Override
		public void lifeCycleStarted (LifeCycle lc) {
			getLogger().info("Server started and listening on port " + getServerPort() + ".");
		}

		@Override
		public void lifeCycleStopping (LifeCycle lc) {
			getLogger().fine("Server stopping...");
		}

		@Override
		public void lifeCycleStopped (LifeCycle lc) {
			getLogger().info("Server stopped.");
			callOnStopRunnable();
		}

		@Override
		public void lifeCycleFailure (LifeCycle lc, Throwable t) {
			getLogger().log(Level.WARNING, "Server failed.", t);
			callOnStopRunnable();
		}

	};

	protected Logger getLogger () {
		return this.logger;
	}

	protected void callOnStopRunnable () {
		if (this.onStopRunnable != null) {
			this.onStopRunnable.run();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
