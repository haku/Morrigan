package net.sparktank.morrigan.server;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.model.exceptions.MorriganException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.webapp.WebAppContext;

public class MorriganServer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Server server;
	
	private Runnable onStopRunnable = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MorriganServer () throws MorriganException {
		try {
			this.server = new Server(8080);
			
			this.server.addLifeCycleListener(this.listener);
			
			WebAppContext webapp = new WebAppContext();
			webapp.setContextPath("/");
			webapp.setWar(Config.getWuiWarLocation());
			
			ContextHandler conMedia = new ContextHandler();
			conMedia.setContextPath("/media");
			conMedia.setResourceBase(".");
			conMedia.setClassLoader(Thread.currentThread().getContextClassLoader());
			conMedia.setHandler(new MediaHandler());
			
			ContextHandler conPlayers = new ContextHandler();
			conPlayers.setContextPath("/player");
			conPlayers.setResourceBase(".");
			conPlayers.setClassLoader(Thread.currentThread().getContextClassLoader());
			conPlayers.setHandler(new PlayersHandler());
			
			ContextHandlerCollection contexts = new ContextHandlerCollection();
	        contexts.setHandlers(new Handler[] { webapp, conMedia, conPlayers });
			this.server.setHandler(contexts);
			
		} catch (Exception e) {
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
