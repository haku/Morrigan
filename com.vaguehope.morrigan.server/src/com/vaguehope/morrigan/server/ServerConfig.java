package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.util.PropertiesFile;

public class ServerConfig implements AuthChecker {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String SERVER_PROPS = "server.properties";
	
	private static final String KEY_PORT = "port";
	private static final int DEFAULT_PORT = 8080;
	
	private static final String KEY_PASS = "pass";
	private static final String DEFAULT_PASS = "Morrigan";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private final PropertiesFile propFile = new PropertiesFile(Config.getConfigDir() + '/' + SERVER_PROPS);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public int getServerPort () throws IOException {
		return this.propFile.getInt(KEY_PORT, DEFAULT_PORT);
	}
	
	public boolean verifyPassword (String passToTest) throws IOException {
		// TODO use bcrypt.
		String pass = this.propFile.getString(KEY_PASS, DEFAULT_PASS);
		return pass.equals(passToTest);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	AuthChecker
	
	@Override
	public boolean verifyAuth (String passToTest) {
		try {
			return verifyPassword(passToTest);
		}
		catch (IOException e) {
			this.logger.log(Level.WARNING, "Failed to verify password.", e);
			return false;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}