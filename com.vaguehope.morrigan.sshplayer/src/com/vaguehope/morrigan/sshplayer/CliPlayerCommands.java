package com.vaguehope.morrigan.sshplayer;

import java.io.File;

public interface CliPlayerCommands {

	int SHARED_PORT = 34400; // TODO auto find free port.

	String startCommand (File media);

	String killCommand ();

	String pauseResumeCommand ();

}
