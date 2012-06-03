package com.vaguehope.morrigan.sshplayer;

import java.io.File;
import java.io.InputStream;

import com.jcraft.jsch.ChannelExec;

public interface CliPlayerCommands {

	int SHARED_PORT = 34400; // TODO auto find free port.

	CliStatusReader makeStatusReader (InputStream source);

	void configureExec (ChannelExec chex, File media);

	String killCommand ();

	CliPlayerCommand pauseResumeCommand ();

}
