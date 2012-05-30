package com.vaguehope.morrigan.sshplayer;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public interface CliPlayerCommand {

	void exec (Session session, ChannelExec mainChEx) throws JSchException;

}
