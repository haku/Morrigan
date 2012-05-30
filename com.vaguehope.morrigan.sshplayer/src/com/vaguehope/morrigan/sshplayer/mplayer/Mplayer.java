package com.vaguehope.morrigan.sshplayer.mplayer;

import java.io.File;
import java.io.InputStream;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.vaguehope.morrigan.sshplayer.CliPlayerCommand;
import com.vaguehope.morrigan.sshplayer.CliPlayerCommands;
import com.vaguehope.morrigan.sshplayer.CliPlayerHelper;
import com.vaguehope.morrigan.sshplayer.CliStatusReader;

/**
 * http://www.mplayerhq.hu/DOCS/tech/slave.txt
 */
public enum Mplayer implements CliPlayerCommands {

	INSTANCE;

	@Override
	public CliStatusReader makeStatusReader (InputStream source) {
		return new MplayerStatusReader(source);
	}

	@Override
	public String startCommand (File media) {
		StringBuilder s = new StringBuilder("cd"); // Start in home directory.
		s.append(" ; export DISPLAY=:0");
		s.append(" ; if [[ ! -e .mnmpcmd ]] ; then mkfifo .mnmpcmd ; fi");
		s.append(" ; mplayer -input file=.mnmpcmd -cache 32768 -cache-min 50 -identify -fs 'http://localhost:")
				.append(CliPlayerCommands.SHARED_PORT)
				.append("/")
				.append(CliPlayerHelper.genericFileName(media))
				.append("'");
		s.append(" ; echo ").append(CliStatusReader.MORRIGAN_EOF);
		return s.toString();
	}

	@Override
	public String killCommand () {
		return "killall mplayer > /dev/null 2>&1"; // Very blunt.
	}

	@Override
	public CliPlayerCommand pauseResumeCommand () {
		return Commands.PAUSE_RESUME;
	}

	private enum Commands implements CliPlayerCommand {
		PAUSE_RESUME {
			@Override
			public void exec (Session session, ChannelExec mainChEx) throws JSchException {
				CliPlayerHelper.execCommand(session, "echo pause > ~/.mnmpcmd"); // Must be a nicer way to do this.
			}
		};

		@Override
		public abstract void exec (Session session, ChannelExec mainChEx) throws JSchException;
	}

}
