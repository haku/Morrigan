package net.sparktank.morrigan.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

public class ServerMain {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public Server makeServer () throws Exception {
		Server server = new Server(8080);
		
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
		
		ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { conHome, conMedia });
		server.setHandler(contexts);
		
		server.start();
		return server;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void main(String[] args) throws Exception {
		ServerMain m = new ServerMain();
		Server s = m.makeServer();
		s.join(); // Like Thread.join().
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
