package com.vaguehope.morrigan.sshplayer;

import java.io.File;
import java.io.InputStream;

public interface CliPlayerCommands {

	int SHARED_PORT = 34400; // TODO auto find free port.

	CliStatusReader makeStatusReader (InputStream source);

	String startCommand (File media);

	String killCommand ();

	CliPlayerCommand pauseResumeCommand ();

}
