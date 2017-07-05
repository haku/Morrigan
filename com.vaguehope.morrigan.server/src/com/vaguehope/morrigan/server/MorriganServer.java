package com.vaguehope.morrigan.server;

import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.server.util.JettyLogger;
import com.vaguehope.morrigan.server.util.WebAppHelper;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.transcode.Transcoder;
import com.vaguehope.morrigan.wui.MorriganWui;

public class MorriganServer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private final Server server;
	private final int serverPort;

	private Runnable onStopRunnable = null;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MorriganServer (final BundleContext context, final ServerConfig config,
			final PlayerReader playerListener, final MediaFactory mediaFactory,
			final AsyncTasksRegister asyncTasksRegister, final AsyncActions asyncActions,
			final Transcoder transcoder,
			final ScheduledExecutorService schEs) throws MorriganException {
		try {
			// Config.
			this.serverPort = config.getServerPort();

			final QueuedThreadPool threadPool = new QueuedThreadPool();
			threadPool.setName("jty");

			// Jetty server instance.
			org.eclipse.jetty.util.log.Log.setLog(new JettyLogger()); // Fix noisy logging.
			this.server = new Server();
			this.server.setThreadPool(threadPool);
			this.server.addLifeCycleListener(this.lifeCycleListener);

			// HTTP connector.
			final Connector connector = new SocketConnector();
			connector.setPort(this.getServerPort());
			this.server.addConnector(connector);

			// HTTPS.
//			final SslContextFactory sslContextFactory = new SslContextFactory(sKeyStore);
//			sslContextFactory.setKeyStorePassword(sPassword);
//			final SslSocketConnector sslConnector = new SslSocketConnector(sslContextFactory);
//			sslConnector.setReuseAddress(true);
//			sslConnector.setPort(8443);
//			this.server.addConnector(sslConnector);

			final FilterHolder authFilterHolder = new FilterHolder(new AuthFilter(config, schEs));

			final WebAppContext warContext = WebAppHelper.getWarBundleAsContext(context, MorriganWui.ID, "/");
			warContext.addFilter(authFilterHolder, "/*", null);
			warContext.addServlet(new ServletHolder(new PlayersServlet(playerListener)), PlayersServlet.CONTEXTPATH + "/*");
			warContext.addServlet(new ServletHolder(new MlistsServlet(playerListener, mediaFactory, asyncActions, transcoder)), MlistsServlet.CONTEXTPATH + "/*");
			warContext.addServlet(new ServletHolder(new StatusServlet(asyncTasksRegister)), StatusServlet.CONTEXTPATH + "/*");
			warContext.addServlet(new ServletHolder(new HostInfoServlet()), HostInfoServlet.CONTEXTPATH + "/*");
			this.server.setHandler(warContext);
		}
		catch (final Exception e) {
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

	public void setOnStopRunnable (final Runnable r) {
		this.onStopRunnable = r;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Jetty Server listener.

	private final Listener lifeCycleListener = new Listener() {

		@Override
		public void lifeCycleStarting (final LifeCycle lc) {
			getLogger().fine("Server starting...");
		}

		@Override
		public void lifeCycleStarted (final LifeCycle lc) {
			getLogger().info("Server started and listening on port " + getServerPort() + ".");
		}

		@Override
		public void lifeCycleStopping (final LifeCycle lc) {
			getLogger().fine("Server stopping...");
		}

		@Override
		public void lifeCycleStopped (final LifeCycle lc) {
			getLogger().info("Server stopped.");
			callOnStopRunnable();
		}

		@Override
		public void lifeCycleFailure (final LifeCycle lc, final Throwable t) {
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
