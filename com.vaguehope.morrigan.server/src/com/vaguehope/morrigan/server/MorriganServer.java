package com.vaguehope.morrigan.server;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
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
	
	private static final int PORT = 8080;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private final Server server;
	
	private Runnable onStopRunnable = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MorriganServer (BundleContext context) throws MorriganException {
		try {
			this.server = new Server();
			Connector connector = new SocketConnector();
			connector.setPort(PORT);
			this.server.addConnector(connector);
			this.server.addLifeCycleListener(this);
			
//			SslSocketConnector sslConnector = new SslSocketConnector();
//			sslConnector.setKeystore(KEYSTORE);
//			sslConnector.setTruststore(KEYSTORE);
//			sslConnector.setPassword(KEYPASSWORD);
//			sslConnector.setKeyPassword(KEYPASSWORD);
//			sslConnector.setTrustPassword(KEYPASSWORD);
//			sslConnector.setMaxIdleTime(30000);
//			sslConnector.setPort(8443);
//			this.server.addConnector(sslConnector);
			
			ContextHandlerCollection contexts = new ContextHandlerCollection();
			this.server.setHandler(contexts);
			
			ServletContextHandler servletContext = new ServletContextHandler(contexts, "/", ServletContextHandler.SESSIONS);
			servletContext.addServlet(new ServletHolder(new PlayersServlet()), PlayersServlet.CONTEXTPATH + "/*");
			servletContext.addServlet(new ServletHolder(new MlistsServlet()), MlistsServlet.CONTEXTPATH + "/*");
			servletContext.addServlet(new ServletHolder(new StatusServlet()), StatusServlet.CONTEXTPATH + "/*");
			
			WebAppContext warContext = WebAppHelper.getWarBundleAsContext(context, MorriganWui.ID, "/");
			contexts.addHandler(warContext);
			
		}
		catch (Exception e) {
			throw new MorriganException("Failed to create server object.", e);
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
	public void lifeCycleStarting(LifeCycle lc) {
		MorriganServer.this.logger.fine("Server starting...");
	}
	
	@Override
	public void lifeCycleStarted(LifeCycle lc) {
		MorriganServer.this.logger.info("Server started and listening on port "+PORT+".");
	}
	
	@Override
	public void lifeCycleStopping(LifeCycle lc) {
		MorriganServer.this.logger.fine("Server stopping...");
	}
	
	@Override
	public void lifeCycleStopped(LifeCycle lc) {
		MorriganServer.this.logger.info("Server stopped.");
		callOnStopRunnable();
	}
	
	@Override
	public void lifeCycleFailure(LifeCycle lc, Throwable t) {
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
