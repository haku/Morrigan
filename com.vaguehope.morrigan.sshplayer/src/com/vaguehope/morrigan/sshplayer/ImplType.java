package com.vaguehope.morrigan.sshplayer;

import com.vaguehope.morrigan.sshplayer.mplayer.Mplayer;

public enum ImplType {

	MPLAYER(Mplayer.INSTANCE);

	private final CliPlayerCommands cmds;

	private ImplType (CliPlayerCommands cmds) {
		this.cmds = cmds;
	}

	public CliPlayerCommands getCmds () {
		return this.cmds;
	}

}
