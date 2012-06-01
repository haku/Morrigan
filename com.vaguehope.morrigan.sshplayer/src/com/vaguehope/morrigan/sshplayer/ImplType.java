package com.vaguehope.morrigan.sshplayer;

import com.vaguehope.morrigan.sshplayer.mplayer.Mplayer;
import com.vaguehope.morrigan.sshplayer.omxplayer.Omxplayer;

public enum ImplType {

	MPLAYER(Mplayer.INSTANCE),
	OMXPLAYER(Omxplayer.INSTANCE),
	;

	private final CliPlayerCommands cmds;

	private ImplType (CliPlayerCommands cmds) {
		this.cmds = cmds;
	}

	public CliPlayerCommands getCmds () {
		return this.cmds;
	}

	public static ImplType parseType (String type) {
		if ("mplayer".equalsIgnoreCase(type)) {
			return MPLAYER;
		}
		else if ("omxplayer".equalsIgnoreCase(type)) {
			return OMXPLAYER;
		}
		return null;
	}

}
