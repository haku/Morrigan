package com.vaguehope.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewritePatternRule;
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
import com.vaguehope.morrigan.dlna.DlnaService;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.rpc.RpcStatusServlet;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.transcode.Transcoder;

public class MorriganServer {

	public static final String REVERSE_PROXY_PREFIX = "/mn";

	private static final Logger LOG = LoggerFactory.getLogger(MorriganServer.class);

	private final Server server;
	private final int serverPort;
	private final ServletContextHandler context;

	private Runnable onStopRunnable = null;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MorriganServer (
			final int httpPort,
			final Collection<String> additionalCorsOrigins,
			final File overrideWebRoot,
			final Config config, final ServerConfig serverConfig,
			final PlayerReader playerListener, final MediaFactory mediaFactory,
			final AsyncTasksRegister asyncTasksRegister, final AsyncActions asyncActions,
			final Transcoder transcoder,
			final ScheduledExecutorService schEs) throws MorriganException {
		try {
			this.serverPort = httpPort;

			final QueuedThreadPool threadPool = new QueuedThreadPool();
			threadPool.setName("jty");
			this.server = new Server(threadPool);
			this.server.addLifeCycleListener(this.lifeCycleListener);
			this.context = makeContentHandler(additionalCorsOrigins, overrideWebRoot, config, serverConfig, playerListener, mediaFactory, asyncTasksRegister, asyncActions, transcoder, schEs);
			this.server.setHandler(wrapWithRewrites(new HandlerList(this.context)));

			final InetAddress bindAddress = serverConfig.getBindAddress("HTTP");
			if (bindAddress == null) throw new IllegalStateException("Failed to find bind address.");
			this.server.addConnector(createHttpConnector(this.server, bindAddress.getHostAddress(), this.serverPort));
		}
		catch (final Exception e) {
			throw new MorriganException("Failed to configure and start server.", e);
		}
	}

	private static Connector createHttpConnector (final Server server, final String hostAddress, final int port) {
		final ServerConnector connector = new ServerConnector(server);
		connector.setHost(hostAddress);
		connector.setPort(port);
		return connector;
	}

	protected static RewriteHandler wrapWithRewrites(final Handler wrapped) {
		final RewriteHandler rewrites = new RewriteHandler();

		// Do not modify the request object because:
		// - RuleContainer.apply() messes up the encoding.
		// - ServletHelper.getReqPath() knows how to remove the prefix.
		rewrites.setRewriteRequestURI(false);
		rewrites.setRewritePathInfo(false);

		rewrites.addRule(new RewritePatternRule(REVERSE_PROXY_PREFIX + "/*", "/"));

		rewrites.setHandler(wrapped);
		return rewrites;
	}

	private static ServletContextHandler makeContentHandler (final Collection<String> additionalCorsOrigins, final File overrideWebRoot, final Config config, final ServerConfig serverConfig, final PlayerReader playerListener, final MediaFactory mediaFactory, final AsyncTasksRegister asyncTasksRegister, final AsyncActions asyncActions, final Transcoder transcoder, final ScheduledExecutorService schEs) throws IOException {
		final ServletContextHandler context = getWuiContext(overrideWebRoot);

		final FilterHolder authFilterHolder = new FilterHolder(new AuthFilter(serverConfig, additionalCorsOrigins, config, schEs));
		context.addFilter(authFilterHolder, "/*", null);

		context.addServlet(new ServletHolder(new LibraryServlet(config)), LibraryServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(new PlayersServlet(playerListener, config)), PlayersServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(
				new MlistsServlet(playerListener, mediaFactory, asyncActions, transcoder, config)),
				MlistsServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(new StatusServlet(asyncTasksRegister)), StatusServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(new HostInfoServlet()), HostInfoServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(new LogServlet()), LogServlet.CONTEXTPATH + "/*");
		context.addServlet(new ServletHolder(new RpcStatusServlet()), RpcStatusServlet.CONTEXTPATH + "/*");

		return context;
	}

	public void enableDlnaCtl(final DlnaService dlnaSvs) {
		this.context.addServlet(new ServletHolder(new DlnaCtlServlet(dlnaSvs)), DlnaCtlServlet.CONTEXTPATH + "/*");
	}

	@SuppressWarnings("resource")
	private static ServletContextHandler getWuiContext(final File overrideWebRoot) {
		final Resource rootRes;
		if (overrideWebRoot != null) {
			rootRes = Resource.newResource(overrideWebRoot);
		}
		else {
			rootRes = classpathRootRes();
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

	private static Resource classpathRootRes() {
		final URL f = MorriganServer.class.getClassLoader().getResource("wui/index.html");
		if (f == null) {
			throw new IllegalStateException("Unable to find wui directory.");
		}
		try {
			final URI rootUri = URI.create(f.toURI().toASCIIString().replaceFirst("/index.html$", "/"));
			return Resource.newResource(rootUri);
		}
		catch (final URISyntaxException | MalformedURLException e) {
			throw new IllegalStateException(e);
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

	protected int getServerPort () {
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
