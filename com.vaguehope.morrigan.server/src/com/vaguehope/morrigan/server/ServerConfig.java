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

	private static final String KEY_PLAYER_ENABLED = "player";
	private static final String DEFAULT_PLAYER_ENABLED = Boolean.TRUE.toString();

	private static final String KEY_BINDIP = "bindip";

	private static final String KEY_AUTOSTART = "autostart";
	private static final String DEFAULT_AUTOSTART = Boolean.FALSE.toString();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected final Logger logger = Logger.getLogger(this.getClass().getName());

	private final PropertiesFile propFile = new PropertiesFile(Config.getConfigDir() + '/' + SERVER_PROPS);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public int getServerPort () throws IOException {
		return this.propFile.getInt(KEY_PORT, DEFAULT_PORT);
	}

	public boolean verifyPassword (final String passToTest) throws IOException {
		// TODO use bcrypt.
		final String pass = this.propFile.getString(KEY_PASS, DEFAULT_PASS);
		return pass.equals(passToTest);
	}

	public boolean isServerPlayerEnabled () throws IOException {
		return Boolean.parseBoolean(this.propFile.getString(KEY_PLAYER_ENABLED, DEFAULT_PLAYER_ENABLED));
	}

	public String getBindIp () throws IOException {
		return this.propFile.getString(KEY_BINDIP, null);
	}

	public boolean isAutoStart () throws IOException {
		return Boolean.parseBoolean(this.propFile.getString(KEY_AUTOSTART, DEFAULT_AUTOSTART));
	}
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	AuthChecker

	@Override
	public boolean verifyAuth (final String passToTest) {
		try {
			return verifyPassword(passToTest);
		}
		catch (final IOException e) {
			this.logger.log(Level.WARNING, "Failed to verify password.", e);
			return false;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
