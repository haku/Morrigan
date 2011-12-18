package com.vaguehope.morrigan.server;

import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.DispatcherType;
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
import com.vaguehope.morrigan.wui.MorriganWui;

public class MorriganServer implements Listener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private final Server server;
	private final int serverPort;
	
	private Runnable onStopRunnable = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MorriganServer (BundleContext context) throws MorriganException {
		try {
			// Config.
			ServerConfig config = new ServerConfig();
			this.serverPort = config.getServerPort();
			
			// Jetty server instance.
			org.eclipse.jetty.util.log.Log.setLog(new JettyLogger()); // Fix noisy logging.
			this.server = new Server();
			this.server.addLifeCycleListener(this);
			
			// HTTP connector.
			Connector connector = new SocketConnector();
			connector.setPort(this.serverPort);
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
			servletContext.addFilter(filterHolder, "/*", (EnumSet<DispatcherType>)null);
			servletContext.addServlet(new ServletHolder(new PlayersServlet()), PlayersServlet.CONTEXTPATH + "/*");
			servletContext.addServlet(new ServletHolder(new MlistsServlet()), MlistsServlet.CONTEXTPATH + "/*");
			servletContext.addServlet(new ServletHolder(new StatusServlet()), StatusServlet.CONTEXTPATH + "/*");
			
			// Web UI in WAR file.
			WebAppContext warContext = WebAppHelper.getWarBundleAsContext(context, MorriganWui.ID, "/");
			warContext.addFilter(filterHolder, "/*", (EnumSet<DispatcherType>) null);
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Listener methods.
	
	@Override
	public void lifeCycleStarting (LifeCycle lc) {
		MorriganServer.this.logger.fine("Server starting...");
	}
	
	@Override
	public void lifeCycleStarted (LifeCycle lc) {
		MorriganServer.this.logger.info("Server started and listening on port " + this.serverPort + ".");
	}
	
	@Override
	public void lifeCycleStopping (LifeCycle lc) {
		MorriganServer.this.logger.fine("Server stopping...");
	}
	
	@Override
	public void lifeCycleStopped (LifeCycle lc) {
		MorriganServer.this.logger.info("Server stopped.");
		callOnStopRunnable();
	}
	
	@Override
	public void lifeCycleFailure (LifeCycle lc, Throwable t) {
		MorriganServer.this.logger.log(Level.WARNING, "Server failed.", t);
		callOnStopRunnable();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	void callOnStopRunnable () {
		if (this.onStopRunnable != null) {
			this.onStopRunnable.run();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setOnStopRunnable (Runnable r) {
		this.onStopRunnable = r;
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
