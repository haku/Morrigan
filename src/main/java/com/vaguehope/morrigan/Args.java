package com.vaguehope.morrigan;

import java.util.List;

import org.kohsuke.args4j.Option;

public class Args {

	@Option(name = "--configpath", usage = "Path to config directory, defaults to $HOME/.morrigan") private String configPath;
	@Option(name = "--dlna", usage = "Enable DLNA.") private boolean dlna;
	@Option(name = "--http", usage = "Local port to bind HTTP UI to.") private int httpPort = -1;
	@Option(name = "--interface", usage = "Hostname or IP address of interface to bind to.") private String iface;
	@Option(name = "--origin", usage = "Hostname or IP address for CORS origin.") private List<String> origins;
	@Option(name = "--ssh", usage = "Local port to bind SSH UI to.") private int sshPort = -1;
	@Option(name = "--vlcarg", metaVar = "--foo=bar", usage = "Extra VLC arg, use once for each arg.") private List<String> vlcArgs;
	@Option(name = "-v", aliases = { "--verbose" }, usage = "print log lines for various events.") private boolean verboseLog = false;

	public String getConfigPath() {
		return this.configPath;
	}

	public String getInterface () {
		return this.iface;
	}

	public List<String> getOrigins() {
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

}
