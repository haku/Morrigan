package com.vaguehope.morrigan.sshui.ssh;

import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import com.vaguehope.morrigan.server.ServerConfig;

public class MnPasswordAuthenticator implements PasswordAuthenticator {

	private final ServerConfig config;

	public MnPasswordAuthenticator (final ServerConfig serverConfig) {
		this.config = serverConfig;
	}

	@Override
	public boolean authenticate (final String username, final String password, final ServerSession session) {
		return this.config.verifyAuth(username, password);
	}

}
