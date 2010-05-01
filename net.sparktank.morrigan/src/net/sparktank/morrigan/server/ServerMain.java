package net.sparktank.morrigan.server;


public class ServerMain {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public MorriganServer makeServer () throws Exception {
		return new MorriganServer();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void main(String[] args) throws Exception {
		MorriganServer s = makeServer();
		s.getServer().start();
		s.getServer().join(); // Like Thread.join().
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
