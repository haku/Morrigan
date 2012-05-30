package com.vaguehope.morrigan.sshplayer;

import java.io.File;

/**
 * http://www.mplayerhq.hu/DOCS/tech/slave.txt
 */
public enum Mplayer implements CliPlayerCommands {

	INSTANCE;

	@Override
	public String startCommand (File media) {
		StringBuilder s = new StringBuilder("cd"); // Start in home directory.
		s.append(" ; export DISPLAY=:0");
		s.append(" ; if [[ ! -e .mnmpcmd ]] ; then mkfifo .mnmpcmd ; fi");
		s.append(" ; mplayer -input file=.mnmpcmd -cache 32768 -cache-min 50 -identify -fs 'http://localhost:")
				.append(CliPlayerCommands.SHARED_PORT)
				.append("/")
				.append(genericFileName(media))
				.append("'");
		s.append(" ; echo ").append(CliStatusReader.MORRIGAN_EOF);
		return s.toString();
	}

	@Override
	public String killCommand () {
		return "killall mplayer > /dev/null 2>&1"; // Very blunt.
	}

	@Override
	public String pauseResumeCommand () {
		return "echo pause > ~/.mnmpcmd"; // Must be a nicer way to do this.
	}

	private static String genericFileName (File file) {
		String n = file.getName();
		return "file" + n.substring(n.lastIndexOf("."));
	}

}
