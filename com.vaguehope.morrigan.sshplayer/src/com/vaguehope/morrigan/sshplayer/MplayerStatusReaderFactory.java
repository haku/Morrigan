package com.vaguehope.morrigan.sshplayer;

import java.io.InputStream;

public enum MplayerStatusReaderFactory implements CliStatusReaderFactory {

	INSTANCE;

	@Override
	public CliStatusReader makeNew (InputStream source) {
		return new MplayerStatusReader(source);
	}

}
