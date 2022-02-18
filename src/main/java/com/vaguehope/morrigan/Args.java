package com.vaguehope.morrigan;

import org.kohsuke.args4j.Option;

public class Args {

	@Option(name = "-p", aliases = { "--port" }, usage = "Local port to bind to.") private int port;
	@Option(name = "-i", aliases = { "--interface" }, usage = "Hostname or IP address of interface to bind to.") private String iface;
	@Option(name = "-v", aliases = { "--verbose" }, usage = "print log lines for various events.") private boolean verboseLog;

	public int getPort() {
		return this.port;
	}

	public String getInterface () {
		return this.iface;
	}

	public boolean isVerboseLog() {
		return this.verboseLog;
	}

}
