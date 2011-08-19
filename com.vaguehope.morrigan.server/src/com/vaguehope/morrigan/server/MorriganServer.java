package com.vaguehope.morrigan.server;


import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.webapp.WebAppContext;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;

public class MorriganServer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Server server;
	
	private Runnable onStopRunnable = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MorriganServer () throws MorriganException {
		try {
			this.server = new Server();
			
			Connector connector = new SocketConnector();
			connector.setPort(8080);
			this.server.addConnector(connector);
			
//			SslSocketConnector sslConnector = new SslSocketConnector();
//			sslConnector.setKeystore(KEYSTORE);
//			sslConnector.setTruststore(KEYSTORE);
//			sslConnector.setPassword(KEYPASSWORD);
//			sslConnector.setKeyPassword(KEYPASSWORD);
//			sslConnector.setTrustPassword(KEYPASSWORD);
//			sslConnector.setMaxIdleTime(30000);
//			sslConnector.setPort(8443);
//			this.server.addConnector(sslConnector);
			
			this.server.addLifeCycleListener(this.listener);
			
			ContextHandlerCollection contexts = new ContextHandlerCollection();
			this.server.setHandler(contexts);
			
			ServletContextHandler servletContext = new ServletContextHandler(contexts, "/", ServletContextHandler.SESSIONS);
			servletContext.addServlet(new ServletHolder(new PlayersServlet()), PlayersServlet.CONTEXTPATH + "/*");
			servletContext.addServlet(new ServletHolder(new MlistsServlet()), MlistsServlet.CONTEXTPATH + "/*");
			
//			final URL warUrl = this.class.getClassLoader().getResource(WEBAPPDIR);
//			final String warUrlString = warUrl.toExternalForm();
//			server.setHandler(new WebAppContext(warUrlString, CONTEXTPATH));
			
			WebAppContext webapp = new WebAppContext(Config.getWuiWarLocation(), "/");
			// This is a hack to fix FileNotFound "org/eclipse/jetty/webapp/webdefault.xml" (in jetty-webapp-7.2.2.v20101205.jar)
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			contexts.addHandler(webapp);
			
			ContextHandler conPlayers = new ContextHandler();
			conPlayers.setContextPath("/player");
			conPlayers.setResourceBase(".");
			conPlayers.setHandler(new PlayersHandler());
			contexts.addHandler(conPlayers);
			
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
	
	private Listener listener = new Listener() {
		
		@Override
		public void lifeCycleStarting(LifeCycle lc) {
			System.err.println("Server starting...");
		}
		
		@Override
		public void lifeCycleStarted(LifeCycle lc) {
			System.err.println("Server started.");
		}
		
		@Override
		public void lifeCycleStopping(LifeCycle lc) {
			System.err.println("Server stopping...");
		}
		
		@Override
		public void lifeCycleStopped(LifeCycle lc) {
			System.err.println("Server stopped.");
			callOnStopRunnable();
		}
		
		@Override
		public void lifeCycleFailure(LifeCycle lc, Throwable t) {
			System.err.println("Server failed.");
			t.printStackTrace();
			callOnStopRunnable();
		}
	};
	
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
