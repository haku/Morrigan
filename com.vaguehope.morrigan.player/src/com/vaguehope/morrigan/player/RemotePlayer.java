package com.vaguehope.morrigan.player;

public interface RemotePlayer extends Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	boolean isAvailable ();

	String getRemoteHost ();
	int getRemotePlayerId ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
