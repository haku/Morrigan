package com.vaguehope.morrigan;

import org.kohsuke.args4j.Option;

public class Args {

	@Option(name = "-d", aliases = { "--dlna" }, usage = "Enable DLNA.") private boolean dlna;
	@Option(name = "-i", aliases = { "--interface" }, usage = "Hostname or IP address of interface to bind to.") private String iface;
	@Option(name = "-p", aliases = { "--port" }, usage = "Local port to bind HTTP UI to.") private int httpPort = -1;
	@Option(name = "-s", aliases = { "--ssh" }, usage = "Local port to bind SSH UI to.") private int sshPort = -1;
	@Option(name = "-v", aliases = { "--verbose" }, usage = "print log lines for various events.") private boolean verboseLog = false;

	public String getInterface () {
		return this.iface;
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

}
