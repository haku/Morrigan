package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.transcode.Transcoder;

public class MorriganServer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final Logger LOG = LoggerFactory.getLogger(MorriganServer.class);

	private final Server server;
	private final int serverPort;

	private Runnable onStopRunnable = null;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MorriganServer (final ServerConfig config,
			final PlayerReader playerListener, final MediaFactory mediaFactory,
			final AsyncTasksRegister asyncTasksRegister, final AsyncActions asyncActions,
			final Transcoder transcoder,
			final ScheduledExecutorService schEs) throws MorriganException {
		try {
			this.serverPort = config.getServerPort();

			final QueuedThreadPool threadPool = new QueuedThreadPool();
			threadPool.setName("jty");
			this.server = new Server(threadPool);
			this.server.addLifeCycleListener(this.lifeCycleListener);
			this.server.setHandler(makeContentHandler(config, playerListener, mediaFactory, asyncTasksRegister, asyncActions, transcoder, schEs));
			this.server.addConnector(createHttpConnector(this.server, this.getServerPort()));
		}
		catch (final Exception e) {
			throw new MorriganException("Failed to configure and start server.", e);
		}
	}

	private static Connector createHttpConnector (final Server server, final int port) {
		final ServerConnector connector = new ServerConnector(server);
//		connector.setHost(iface);  // TODO
		connector.setPort(port);
		return connector;
	}

	private static HandlerList makeContentHandler (final ServerConfig config, final PlayerReader playerListener, final MediaFactory mediaFactory, final AsyncTasksRegister asyncTasksRegister, final AsyncActions asyncActions, final Transcoder transcoder, final ScheduledExecutorService schEs) throws IOException {
		final ServletContextHandler context = getWuiContext();

		final FilterHolder authFilterHolder = new FilterHolder(new AuthFilter(config, Config.DEFAULT, schEs));
		context.addFilter(authFilterHolder, "/*", null);

		context.addServlet(new ServletHolder(new LibraryServlet(Config.DEFAULT)), LibraryServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(new PlayersServlet(playerListener)), PlayersServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(
				new MlistsServlet(playerListener, mediaFactory, asyncActions, transcoder, Config.DEFAULT)),
				MlistsServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(new StatusServlet(asyncTasksRegister)), StatusServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(new HostInfoServlet()), HostInfoServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(new LogServlet()), LogServlet.CONTEXTPATH + "/*");

		final HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { context });
		return handlers;
	}

	private static ServletContextHandler getWuiContext() {
		final URL f = MorriganServer.class.getClassLoader().getResource("wui/index.html");
		if (f == null) {
			throw new IllegalStateException("Unable to find wui directory.");
		}

		final Resource rootRes;
		try {
			final URI rootUri = URI.create(f.toURI().toASCIIString().replaceFirst("/index.html$", "/"));
			rootRes = Resource.newResource(rootUri);
		}
		catch (final URISyntaxException | MalformedURLException e) {
			throw new IllegalStateException(e);
		}

		final ResourceService resourceService = new ResourceService();
		resourceService.setDirAllowed(true);

		final ServletContextHandler context = new ServletContextHandler();
//		MediaFormat.addTo(servletHandler.getMimeTypes());

		context.setContextPath("/");
		context.setBaseResource(rootRes);
		context.setWelcomeFiles(new String[] { "index.html" });
		context.addServlet(new ServletHolder(new DefaultServlet(resourceService)), "/");
		return context;
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
			LOG.debug("Server starting...");
		}

		@Override
		public void lifeCycleStarted (final LifeCycle lc) {
			LOG.info("Server started and listening on port " + getServerPort() + ".");
		}

		@Override
		public void lifeCycleStopping (final LifeCycle lc) {
			LOG.debug("Server stopping...");
		}

		@Override
		public void lifeCycleStopped (final LifeCycle lc) {
			LOG.info("Server stopped.");
			callOnStopRunnable();
		}

		@Override
		public void lifeCycleFailure (final LifeCycle lc, final Throwable t) {
			LOG.warn("Server failed.", t);
			callOnStopRunnable();
		}

	};

	protected void callOnStopRunnable () {
		if (this.onStopRunnable != null) {
			this.onStopRunnable.run();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
