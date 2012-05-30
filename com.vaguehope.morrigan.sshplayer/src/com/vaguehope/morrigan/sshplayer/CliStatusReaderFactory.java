package com.vaguehope.morrigan.sshplayer;

import java.io.InputStream;

public interface CliStatusReaderFactory {

	CliStatusReader makeNew (InputStream source);

}
