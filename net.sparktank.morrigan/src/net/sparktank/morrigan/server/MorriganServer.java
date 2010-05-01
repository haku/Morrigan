package net.sparktank.morrigan.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;

public class MorriganServer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Server server;
	
	private Runnable onStopRunnable = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MorriganServer () {
		server = new Server(8080);
		
		server.addLifeCycleListener(listener);
		
		ContextHandler conHome = new ContextHandler();
		conHome.setContextPath("/");
		conHome.setResourceBase(".");
		conHome.setClassLoader(Thread.currentThread().getContextClassLoader());
		conHome.setHandler(new HomeHandler());
		
		ContextHandler conMedia = new ContextHandler();
		conMedia.setContextPath("/media");
		conMedia.setResourceBase(".");
		conMedia.setClassLoader(Thread.currentThread().getContextClassLoader());
		conMedia.setHandler(new MediaHandler());
		
		ContextHandler conPlayers = new ContextHandler();
		conPlayers.setContextPath("/players");
		conPlayers.setResourceBase(".");
		conPlayers.setClassLoader(Thread.currentThread().getContextClassLoader());
		conPlayers.setHandler(new PlayersHandler());
		
		ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { conHome, conMedia, conPlayers });
		server.setHandler(contexts);
	}
	
	private Listener listener = new Listener() {
		
		@Override
		public void lifeCycleStarting(LifeCycle arg0) {
			System.err.println("Server starting...");
		}
		
		@Override
		public void lifeCycleStarted(LifeCycle arg0) {
			System.err.println("Server started.");
		}
		
		@Override
		public void lifeCycleStopping(LifeCycle arg0) {
			System.err.println("Server stopping...");
		}
		
		@Override
		public void lifeCycleStopped(LifeCycle arg0) {
			System.err.println("Server stopped.");
			callOnStopRunnable();
		}
		
		@Override
		public void lifeCycleFailure(LifeCycle arg0, Throwable arg1) {
			System.err.println("Server failed.");
			callOnStopRunnable();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void callOnStopRunnable () {
		if (onStopRunnable != null) {
			onStopRunnable.run();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public Server getServer () {
		return server;
	}
	
	public void setOnStopRunnable (Runnable r) {
		this.onStopRunnable = r;
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
