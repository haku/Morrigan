package com.vaguehope.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.Args;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.util.NetHelper;
import com.vaguehope.morrigan.util.NetHelper.IfaceAndAddr;
import com.vaguehope.morrigan.util.PropertiesFile;

public class ServerConfig implements AuthChecker {

	private static final String SERVER_PROPS = "server.properties";  // TODO move to Config class.

	private static final String KEY_PASS = "pass";
	private static final String DEFAULT_PASS = "Morrigan";

	private static final String KEY_PLAYER_ENABLED = "player";
	private static final String DEFAULT_PLAYER_ENABLED = Boolean.TRUE.toString();

	private static final String KEY_BINDIP = "bindip";

	private static final String KEY_AUTOSTART = "autostart";
	private static final String DEFAULT_AUTOSTART = Boolean.FALSE.toString();

	private static final Logger LOG = LoggerFactory.getLogger(ServerConfig.class);

	private final PropertiesFile propFile;
	private final Args args;

	public ServerConfig(final Config config, final Args args) {
		this.propFile = new PropertiesFile(new File(config.getConfigDir(), SERVER_PROPS));
		this.args = args;
	}

	public boolean verifyPassword (final String passToTest) throws IOException {
		// TODO use bcrypt.
		final String pass = this.propFile.getString(KEY_PASS, DEFAULT_PASS);
		return pass.equals(passToTest);
	}

	public boolean isServerPlayerEnabled () throws IOException {
		return Boolean.parseBoolean(this.propFile.getString(KEY_PLAYER_ENABLED, DEFAULT_PLAYER_ENABLED));
	}

	public InetAddress getBindAddress(final String whatFor) throws IOException {
		String strIface = this.args.getInterface();
		if (strIface == null) {
			strIface = this.propFile.getString(KEY_BINDIP, null);
		}

		final InetAddress ret;
		if (strIface != null) {
			ret = InetAddress.getByName(strIface);
			LOG.info("{} using address: {}", whatFor, ret);
		}
		else {
			final List<IfaceAndAddr> addresses = NetHelper.getIpAddresses();
			ret = addresses.iterator().next().getAddr();
			LOG.info("addresses: {}, {} using address: {}", addresses, whatFor, ret);
		}
		return ret;
	}

	public boolean isAutoStart () throws IOException {
		return Boolean.parseBoolean(this.propFile.getString(KEY_AUTOSTART, DEFAULT_AUTOSTART));
	}

	//	AuthChecker

	@Override
	public boolean verifyAuth (final String passToTest) {
		try {
			return verifyPassword(passToTest);
		}
		catch (final IOException e) {
			LOG.warn("Failed to verify password.", e);
			return false;
		}
	}

}
