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

	private static final Logger LOG = LoggerFactory.getLogger(ServerConfig.class);

	private final PropertiesFile propFile;
	private final Args args;

	public ServerConfig(final Config config, final Args args) {
		this.propFile = new PropertiesFile(new File(config.getConfigDir(), SERVER_PROPS));
		this.args = args;
	}

	public boolean verifyUsername(String username) {
		return true;  // Accept all usernames.
	}

	public boolean verifyPassword (final String passToTest) throws IOException {
		// TODO use bcrypt.
		final String pass = this.propFile.getString(KEY_PASS, DEFAULT_PASS);
		return pass.equals(passToTest);
	}

	public InetAddress getBindAddress(final String whatFor) throws IOException {
		final String strIface = this.args.getInterface();
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

	//	AuthChecker

	@Override
	public boolean verifyAuth(String user, String pass) {
		try {
			return verifyUsername(user) && verifyPassword(pass);
		}
		catch (final IOException e) {
			LOG.warn("Failed to verify password.", e);
			return false;
		}
	}

}
