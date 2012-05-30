package com.vaguehope.morrigan.sshplayer;

import java.io.File;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public final class CliPlayerHelper {

	private CliPlayerHelper () {}

	public static String genericFileName (File file) {
		String n = file.getName();
		return "file" + n.substring(n.lastIndexOf("."));
	}

	public static void execCommand (Session session, String command) throws JSchException {
		ChannelExec cmdExCh = (ChannelExec) session.openChannel("exec");
		cmdExCh.setCommand(command);
		cmdExCh.connect();
		cmdExCh.disconnect();
	}


}
