package com.vaguehope.morrigan.sshplayer;

public interface CliStatusReader {

	void start ();

	int getCurrentPosition ();

	int getDuration ();

	boolean isFinished ();

}
