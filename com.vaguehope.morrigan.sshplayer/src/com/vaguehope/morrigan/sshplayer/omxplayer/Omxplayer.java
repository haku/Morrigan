package com.vaguehope.morrigan.sshplayer.omxplayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.vaguehope.morrigan.sshplayer.CliPlayerCommand;
import com.vaguehope.morrigan.sshplayer.CliPlayerCommands;
import com.vaguehope.morrigan.sshplayer.CliPlayerHelper;
import com.vaguehope.morrigan.sshplayer.CliStatusReader;

public enum Omxplayer implements CliPlayerCommands {

	INSTANCE;

	@Override
	public CliStatusReader makeStatusReader (InputStream source) {
		return new OmxplayerStatusReader(source);
	}

	@Override
	public void configureExec (ChannelExec chex, File media) {
		StringBuilder s = new StringBuilder("cd"); // Start in home directory.
		s.append(" ; export DISPLAY=:0");
		s.append(" ; omxplayer -s 'http://localhost:")
				.append(CliPlayerCommands.SHARED_PORT)
				.append("/")
				.append(CliPlayerHelper.genericFileName(media))
				.append("'");
		s.append(" ; echo ").append(CliStatusReader.MORRIGAN_EOF);
		chex.setCommand(s.toString());
		chex.setPty(true);
	}

	@Override
	public String killCommand () {
		return "killall omxplayer > /dev/null 2>&1"; // Very blunt.
	}

	@Override
	public CliPlayerCommand pauseResumeCommand () {
		return Commands.PAUSE_RESUME;
	}

	private enum Commands implements CliPlayerCommand {
		PAUSE_RESUME {
			@Override
			public void exec (Session session, ChannelExec mainChEx) throws JSchException, IOException {
				OutputStream os = mainChEx.getOutputStream();
				os.write("p".getBytes());
				os.flush();
			}
		};

		@Override
		public abstract void exec (Session session, ChannelExec mainChEx) throws JSchException, IOException;
	}

}
