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
		conPlayers.setContextPath("/player");
		conPlayers.setResourceBase(".");
		conPlayers.setClassLoader(Thread.currentThread().getContextClassLoader());
		conPlayers.setHandler(new PlayersHandler());
		
		ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { conHome, conMedia, conPlayers });
		server.setHandler(contexts);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void start () throws Exception {
		server.start();
	}
	
	public void stop () throws Exception {
		server.stop();
	}
	
	public void join () throws InterruptedException {
		server.join();
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
	
	private void callOnStopRunnable () {
		if (onStopRunnable != null) {
			onStopRunnable.run();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setOnStopRunnable (Runnable r) {
		this.onStopRunnable = r;
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
