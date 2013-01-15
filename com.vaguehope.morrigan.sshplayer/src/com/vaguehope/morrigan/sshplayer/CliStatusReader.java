package com.vaguehope.morrigan.sshplayer;

public interface CliStatusReader {

	String MORRIGAN_EOF = "Morrigan-EOF";

	void start ();

	int getCurrentPosition ();

	int getDuration ();

	boolean isFinished ();

}
