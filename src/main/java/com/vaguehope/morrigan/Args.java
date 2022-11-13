package com.vaguehope.morrigan;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kohsuke.args4j.Option;

public class Args {

	@Option(name = "--configpath", usage = "Path to config directory, defaults to $HOME/.morrigan") private String configPath;
	@Option(name = "--dlna", usage = "Enable DLNA.") private boolean dlna;
	@Option(name = "--http", usage = "Local port to bind HTTP UI to.") private int httpPort = -1;
	@Option(name = "--interface", usage = "Hostname or IP address of interface to bind to.") private String iface;
	@Option(name = "--origin", usage = "Hostname or IP address for CORS origin.") private List<String> origins;
	@Option(name = "--ssh", usage = "Local port to bind SSH UI to.") private int sshPort = -1;
	@Option(name = "--sshinterface", usage = "Hostname or IP address of interface to bind to, supersedes --interface for ssh.") private List<String> sshIfaces;
	@Option(name = "--vlcarg", metaVar = "--foo=bar", usage = "Extra VLC arg, use once for each arg.") private List<String> vlcArgs;
	@Option(name = "--webroot", usage = "Override static file location, useful for UI dev.") private String webRoot;
	@Option(name = "-v", aliases = { "--verbose" }, usage = "print log lines for various events.") private boolean verboseLog = false;

	public String getConfigPath() {
		return this.configPath;
	}

	public String getInterface () {
		return this.iface;
	}

	/**
	 * Returns null for no interfaces.
	 * Never returns an empty list.
	 */
	public List<InetAddress> getSshInterfaces() throws UnknownHostException {
		if (this.sshIfaces == null || this.sshIfaces.size() < 1) return null;
		final List<InetAddress> ret = new ArrayList<>();
		for (final String i : this.sshIfaces) {
			ret.add(InetAddress.getByName(i));
		}
		return Collections.unmodifiableList(ret);
	}

	public List<String> getOrigins() {
		if (this.origins == null) return Collections.emptyList();
		return this.origins;
	}

	public int getHttpPort() {
		return this.httpPort;
	}

	public int getSshPort() {
		return this.sshPort;
	}

	public boolean isDlna() {
		return this.dlna;
	}

	public boolean isVerboseLog() {
		return this.verboseLog;
	}

	public List<String> getVlcArgs() {
		return this.vlcArgs;
	}

	public File getWebRoot() throws ArgsException {
		return checkIsDirOrNull(this.webRoot);
	}

	private static File checkIsDirOrNull(final String path) throws ArgsException {
		if (path == null) return null;
		final File f = new File(path);
		if (!f.exists()) throw new ArgsException("Not found: " + f.getAbsolutePath());
		if (!f.isDirectory()) throw new ArgsException("Not directory: " + f.getAbsolutePath());
		return f;
	}

	public static class ArgsException extends Exception {
		private static final long serialVersionUID = 4160594293982918286L;
		public ArgsException(final String msg) {
			super(msg);
		}
	}

}
