package com.vaguehope.morrigan.sshplayer;

import java.util.logging.Logger;

import com.jcraft.jsch.UserInfo;

public class MplayerHost {

	protected static final Logger LOG = Logger.getLogger(MplayerHost.class.getName());

	private final String name;
	private final String host;
	private final int port;
	private final String user;
	private final String pass;

	public MplayerHost (String name, String host, int port, String user, String pass) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.user = user;
		this.pass = pass;
	}

	public String getName () {
		return this.name;
	}

	public String getHost () {
		return this.host;
	}

	public int getPort () {
		return this.port;
	}

	public String getUser () {
		return this.user;
	}

	public String getPass () {
		return this.pass;
	}

	public UserInfo getUserinfo () {
		return this.userInfo;
	}

	private final UserInfo userInfo = new UserInfo() {

		@Override
		public void showMessage (String message) {
			LOG.info("Msg from host: " + message);
		}

		@Override
		public boolean promptYesNo (String message) {
			return false;
		}

		@Override
		public boolean promptPassword (String message) {
			return true;
		}

		@Override
		public boolean promptPassphrase (String message) {
			return false;
		}

		@Override
		public String getPassword () {
			return getPass();
		}

		@Override
		public String getPassphrase () {
			return null;
		}
	};

}
